package com.callcenter.core.service.impl;

import com.callcenter.common.event.EventPublisher;
import com.callcenter.common.exception.BusinessException;
import com.callcenter.common.exception.ErrorCode;
import com.callcenter.core.config.CoreProperties;
import com.callcenter.core.config.FreeSwitchConfig;
import com.callcenter.core.event.CallCreatedEvent;
import com.callcenter.core.event.CallEndedEvent;
import com.callcenter.core.service.FreeSwitchConnectionStatus;
import com.callcenter.core.service.FreeSwitchService;
import com.callcenter.core.service.ManagedCallFilterService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import link.thingscloud.freeswitch.esl.IEslEventListener;
import link.thingscloud.freeswitch.esl.InboundClient;
import link.thingscloud.freeswitch.esl.ServerConnectionListener;
import link.thingscloud.freeswitch.esl.inbound.option.ConnectState;
import link.thingscloud.freeswitch.esl.inbound.option.InboundClientOption;
import link.thingscloud.freeswitch.esl.inbound.option.ServerOption;
import link.thingscloud.freeswitch.esl.transport.CommandResponse;
import link.thingscloud.freeswitch.esl.transport.event.EslEvent;
import link.thingscloud.freeswitch.esl.transport.message.EslMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 基于 thingscloud ESL 客户端的 FreeSWITCH 连接管理实现。
 */
@Service
public class FreeSwitchServiceImpl implements FreeSwitchService {

    private static final Logger log = LoggerFactory.getLogger(FreeSwitchServiceImpl.class);
    private static final String EVENT_NAME = "Event-Name";
    private static final String CALL_DIRECTION = "Call-Direction";
    private static final String CHANNEL_CREATE = "CHANNEL_CREATE";
    private static final String CHANNEL_HANGUP_COMPLETE = "CHANNEL_HANGUP_COMPLETE";
    private static final String HEADER_CHANNEL_CALL_UUID = "Channel-Call-UUID";
    private static final String HEADER_UNIQUE_ID = "Unique-ID";
    private static final String HEADER_CALLER = "Caller-Caller-ID-Number";
    private static final String HEADER_CALLEE = "Caller-Destination-Number";
    private static final String HEADER_EVENT_DATE_TIMESTAMP = "Event-Date-Timestamp";
    private static final String HEADER_HANGUP_CAUSE = "variable_hangup_cause";
    private static final String HEADER_HANGUP_CAUSE_FALLBACK = "Hangup-Cause";
    private static final String HEADER_CREATED_TIME = "Caller-Channel-Created-Time";
    private static final String HEADER_PROGRESS_TIME = "Caller-Channel-Progress-Time";
    private static final String HEADER_PROGRESS_MEDIA_TIME = "Caller-Channel-Progress-Media-Time";
    private static final String HEADER_ANSWERED_TIME = "Caller-Channel-Answered-Time";
    private static final String HEADER_HANGUP_TIME = "Caller-Channel-Hangup-Time";
    private static final String HEADER_VARIABLE_DURATION = "variable_duration";
    private static final String HEADER_DURATION_FALLBACK = "variable_billsec";

    private final CoreProperties coreProperties;
    private final FreeSwitchConfig freeSwitchConfig;
    private final EventPublisher eventPublisher;
    private final ManagedCallFilterService managedCallFilterService;
    private final InboundClientFactory inboundClientFactory;
    private final Sleeper sleeper;
    private final Set<String> subscribedEvents = new CopyOnWriteArraySet<>();
    private final Set<String> acceptedManagedCallIds = new CopyOnWriteArraySet<>();
    private final AtomicInteger lastConnectAttempts = new AtomicInteger();

    private volatile InboundClient inboundClient;
    private volatile ConnectState connectState = ConnectState.INIT;
    private volatile boolean degraded;
    private volatile String lastErrorMessage;

    /**
     * 创建生产环境 FreeSWITCH 服务。
     *
     * @param coreProperties core properties
     * @param freeSwitchConfig freeswitch config
     */
    @Autowired
    public FreeSwitchServiceImpl(
            CoreProperties coreProperties,
            FreeSwitchConfig freeSwitchConfig,
            EventPublisher eventPublisher,
            ManagedCallFilterService managedCallFilterService
    ) {
        this(
                coreProperties,
                freeSwitchConfig,
                eventPublisher,
                managedCallFilterService,
                InboundClient::newInstance,
                Thread::sleep
        );
    }

    FreeSwitchServiceImpl(
            CoreProperties coreProperties,
            FreeSwitchConfig freeSwitchConfig,
            EventPublisher eventPublisher,
            ManagedCallFilterService managedCallFilterService,
            InboundClientFactory inboundClientFactory,
            Sleeper sleeper
    ) {
        this.coreProperties = coreProperties;
        this.freeSwitchConfig = freeSwitchConfig;
        this.eventPublisher = eventPublisher;
        this.managedCallFilterService = managedCallFilterService;
        this.inboundClientFactory = inboundClientFactory;
        this.sleeper = sleeper;
    }

    /**
     * 启动时初始化 ESL 客户端；允许在 mock 模式下降级启动。
     */
    @PostConstruct
    void initialize() {
        if (!freeSwitchConfig.isEnabled()) {
            degraded = true;
            lastErrorMessage = "配置已禁用FreeSWITCH连接。";
            log.warn("FreeSWITCH连接已禁用。");
            return;
        }
        if (!freeSwitchConfig.hasRequiredConnectionInfo()) {
            degraded = true;
            lastErrorMessage = "缺少FreeSWITCH host/port/password 配置。";
            log.warn("FreeSWITCH配置不完整，系统以降级模式启动。");
            return;
        }
        try {
            ensureConnected(false);
        } catch (BusinessException ex) {
            if (coreProperties.isMockFsEnabled()) {
                degraded = true;
                lastErrorMessage = ex.getMessage();
                log.warn("FreeSWITCH启动探测失败，系统以降级模式继续启动，原因={}", ex.getMessage());
            } else {
                throw ex;
            }
        }
    }

    /**
     * 停止时关闭 ESL 客户端资源。
     */
    @PreDestroy
    public void shutdown() {
        InboundClient client = inboundClient;
        if (client != null) {
            try {
                client.shutdown();
            } catch (RuntimeException ex) {
                log.warn("FreeSWITCH客户端关闭失败，原因={}", ex.getMessage());
            }
        }
        connectState = ConnectState.SHUTDOWN;
    }

    @Override
    public String originate(String caller, String callee) {
        if (caller == null || caller.isBlank() || callee == null || callee.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "caller/callee must not be blank");
        }
        InboundClient client = ensureConnected(true);
        String dialString = String.format(freeSwitchConfig.getOriginateTemplate(), callee);
        String originateArgs = String.format(
                "{origination_caller_id_number=%s}%s &park()",
                caller,
                dialString
        );
        try {
            String jobId = client.sendAsyncApiCommand(
                    freeSwitchConfig.getServerAddress(),
                    "bgapi",
                    "originate " + originateArgs
            );
            if (jobId == null || jobId.isBlank()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "FreeSWITCH originate returned empty job id");
            }
            return jobId;
        } catch (RuntimeException ex) {
            throw wrapAsBusinessException("Failed to originate call via FreeSWITCH", ex);
        }
    }

    @Override
    public String sendCommand(String command, String arguments) {
        if (command == null || command.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "command must not be blank");
        }
        InboundClient client = ensureConnected(true);
        try {
            EslMessage response = client.sendSyncApiCommand(
                    freeSwitchConfig.getServerAddress(),
                    command,
                    arguments == null ? "" : arguments,
                    freeSwitchConfig.getTimeoutSeconds() * 1000L
            );
            List<String> bodyLines = response.getBodyLines();
            if (bodyLines != null && !bodyLines.isEmpty()) {
                return String.join(System.lineSeparator(), bodyLines);
            }
            return response.toString();
        } catch (RuntimeException ex) {
            throw wrapAsBusinessException("Failed to send FreeSWITCH command: " + command, ex);
        }
    }

    @Override
    public void subscribeEvents(String... events) {
        List<String> normalizedEvents = Arrays.stream(events == null ? new String[0] : events)
                .filter(event -> event != null && !event.isBlank())
                .distinct()
                .collect(Collectors.toList());
        if (normalizedEvents.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "At least one event is required");
        }
        InboundClient client = ensureConnected(true);
        try {
            CommandResponse response = client.setEventSubscriptions(
                    freeSwitchConfig.getServerAddress(),
                    freeSwitchConfig.getEventFormat(),
                    String.join(" ", normalizedEvents)
            );
            if (!response.isOk()) {
                throw new BusinessException(
                        ErrorCode.INTERNAL_ERROR,
                        "FreeSWITCH event subscription failed: " + response.getReplyText()
                );
            }
            subscribedEvents.addAll(normalizedEvents);
        } catch (RuntimeException ex) {
            if (ex instanceof BusinessException businessException) {
                throw businessException;
            }
            throw wrapAsBusinessException("Failed to subscribe FreeSWITCH events", ex);
        }
    }

    @Override
    public FreeSwitchConnectionStatus getStatus() {
        return new FreeSwitchConnectionStatus(
                freeSwitchConfig.isEnabled(),
                freeSwitchConfig.hasRequiredConnectionInfo(),
                degraded,
                isConnected(),
                freeSwitchConfig.getServerAddress(),
                connectState.name(),
                lastConnectAttempts.get(),
                lastErrorMessage,
                new ArrayList<>(subscribedEvents)
        );
    }

    void handleInboundEvent(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return;
        }
        try {
            String eventName = valueOf(headers, EVENT_NAME);
            String callId = callIdOf(headers);
            String caller = valueOf(headers, HEADER_CALLER);
            String callee = valueOf(headers, HEADER_CALLEE);
            if (CHANNEL_CREATE.equalsIgnoreCase(eventName) && isInbound(headers)) {
                if (!shouldProcessEvent(eventName, callId, caller, callee, false)) {
                    return;
                }
                publishCallCreated(headers, callId, caller, callee);
                if (callId != null) {
                    acceptedManagedCallIds.add(callId);
                }
                return;
            }
            if (CHANNEL_HANGUP_COMPLETE.equalsIgnoreCase(eventName)) {
                if (!shouldProcessEvent(
                        eventName,
                        callId,
                        caller,
                        callee,
                        callId != null && acceptedManagedCallIds.contains(callId)
                )) {
                    return;
                }
                publishCallEnded(headers, callId, caller, callee);
                if (callId != null) {
                    acceptedManagedCallIds.remove(callId);
                }
            }
        } catch (RuntimeException ex) {
            log.warn("解析FreeSWITCH事件失败，事件名={}，原因={}", valueOf(headers, EVENT_NAME), ex.getMessage());
        }
    }

    private boolean shouldProcessEvent(
            String eventName,
            String callId,
            String caller,
            String callee,
            boolean alreadyAccepted
    ) {
        if (alreadyAccepted) {
            return true;
        }
        ManagedCallFilterService.ManagedCallDecision decision = managedCallFilterService.evaluate(callId, caller, callee);
        if (!decision.accepted()) {
            log.info(
                    "忽略非受管呼叫事件，event={}, callId={}, caller={}, callee={}, 原因={}",
                    eventName,
                    callId,
                    caller,
                    callee,
                    decision.reason()
            );
            return false;
        }
        log.debug(
                "受管呼叫事件通过过滤，event={}, callId={}, extensionNo={}, agentId={}",
                eventName,
                callId,
                decision.extensionNo(),
                decision.agentId()
        );
        return true;
    }

    private synchronized InboundClient ensureConnected(boolean failIfUnavailable) {
        if (isConnected() && inboundClient != null) {
            return inboundClient;
        }
        if (!freeSwitchConfig.isEnabled()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "FreeSWITCH integration is disabled");
        }
        if (!freeSwitchConfig.hasRequiredConnectionInfo()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "FreeSWITCH configuration is incomplete");
        }

        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= freeSwitchConfig.getMaxRetryAttempts(); attempt++) {
            lastConnectAttempts.set(attempt);
            InboundClient candidate = null;
            try {
                connectState = ConnectState.CONNECTING;
                candidate = inboundClientFactory.create(buildClientOption());
                candidate.start();
                inboundClient = candidate;
                degraded = false;
                lastErrorMessage = null;
                if (connectState == ConnectState.CONNECTING) {
                    connectState = ConnectState.AUTHED;
                }
                return candidate;
            } catch (RuntimeException ex) {
                lastFailure = ex;
                connectState = ConnectState.FAILED;
                lastErrorMessage = ex.getMessage();
                log.warn("FreeSWITCH连接失败，第{}/{}次尝试，原因={}",
                        attempt, freeSwitchConfig.getMaxRetryAttempts(), ex.getMessage());
                closeQuietly(candidate);
                sleepBeforeRetry(attempt);
            }
        }

        BusinessException businessException = wrapAsBusinessException(
                "Failed to connect to FreeSWITCH after " + freeSwitchConfig.getMaxRetryAttempts() + " attempts",
                lastFailure
        );
        if (failIfUnavailable || !coreProperties.isMockFsEnabled()) {
            throw businessException;
        }
        degraded = true;
        lastErrorMessage = businessException.getMessage();
        return null;
    }

    private InboundClientOption buildClientOption() {
        ServerOption serverOption = new ServerOption(freeSwitchConfig.getHost(), freeSwitchConfig.getPort())
                .password(freeSwitchConfig.getPassword())
                .timeoutSeconds(freeSwitchConfig.getTimeoutSeconds());

        InboundClientOption option = new InboundClientOption()
                .defaultPassword(freeSwitchConfig.getPassword())
                .defaultTimeoutSeconds(freeSwitchConfig.getTimeoutSeconds())
                .readTimeoutSeconds(freeSwitchConfig.getHeartbeatTimeoutSeconds())
                .readerIdleTimeSeconds(freeSwitchConfig.getHeartbeatTimeoutSeconds())
                .serverConnectionListener(new ConnectionListener())
                .addListener(new InboundEslEventListener())
                .addServerOption(serverOption);
        if (!freeSwitchConfig.getStartupEvents().isEmpty()) {
            option.addEvents(freeSwitchConfig.getStartupEvents().toArray(String[]::new));
            subscribedEvents.addAll(freeSwitchConfig.getStartupEvents());
        }
        return option;
    }

    private boolean isConnected() {
        return connectState == ConnectState.CONNECTED || connectState == ConnectState.AUTHED;
    }

    private void sleepBeforeRetry(int currentAttempt) {
        if (currentAttempt >= freeSwitchConfig.getMaxRetryAttempts()) {
            return;
        }
        try {
            sleeper.sleep(freeSwitchConfig.getRetryIntervalMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Interrupted while retrying FreeSWITCH connection");
        }
    }

    private void closeQuietly(InboundClient candidate) {
        if (candidate == null) {
            return;
        }
        try {
            candidate.shutdown();
        } catch (RuntimeException ex) {
            log.debug("连接失败后关闭FreeSWITCH客户端时忽略异常，原因={}", ex.getMessage());
        }
    }

    private void publishCallCreated(Map<String, String> headers, String callId, String caller, String callee) {
        if (callId == null) {
            log.warn("忽略缺少callId的CHANNEL_CREATE事件。");
            return;
        }
        eventPublisher.publish(new CallCreatedEvent(
                callId,
                caller,
                callee,
                timestampOf(headers),
                1
        ));
    }

    private void publishCallEnded(Map<String, String> headers, String callId, String caller, String callee) {
        if (callId == null) {
            log.warn("忽略缺少callId的CHANNEL_HANGUP_COMPLETE事件。");
            return;
        }
        eventPublisher.publish(new CallEndedEvent(
                callId,
                caller,
                callee,
                firstNonBlank(valueOf(headers, HEADER_HANGUP_CAUSE), valueOf(headers, HEADER_HANGUP_CAUSE_FALLBACK)),
                endTimestampOf(headers),
                callTypeOf(headers),
                preciseTimestampOf(headers, HEADER_CREATED_TIME),
                ringingTimestampOf(headers),
                preciseTimestampOf(headers, HEADER_ANSWERED_TIME),
                durationOf(headers)
        ));
    }

    private String callIdOf(Map<String, String> headers) {
        return firstNonBlank(valueOf(headers, HEADER_CHANNEL_CALL_UUID), valueOf(headers, HEADER_UNIQUE_ID));
    }

    private LocalDateTime timestampOf(Map<String, String> headers) {
        String rawTimestamp = valueOf(headers, HEADER_EVENT_DATE_TIMESTAMP);
        return timestampFromRaw(rawTimestamp);
    }

    private LocalDateTime preciseTimestampOf(Map<String, String> headers, String key) {
        return timestampFromRaw(valueOf(headers, key));
    }

    private LocalDateTime ringingTimestampOf(Map<String, String> headers) {
        return timestampFromRaw(firstNonBlank(
                valueOf(headers, HEADER_PROGRESS_TIME),
                valueOf(headers, HEADER_PROGRESS_MEDIA_TIME)
        ));
    }

    private LocalDateTime endTimestampOf(Map<String, String> headers) {
        return timestampFromRaw(firstNonBlank(
                valueOf(headers, HEADER_HANGUP_TIME),
                valueOf(headers, HEADER_EVENT_DATE_TIMESTAMP)
        ));
    }

    private LocalDateTime timestampFromRaw(String rawTimestamp) {
        if (rawTimestamp == null) {
            return null;
        }
        long timestamp = Long.parseLong(rawTimestamp);
        if (timestamp <= 0L) {
            return null;
        }
        long epochMillis = timestamp > 10_000_000_000_000L ? timestamp / 1000L : timestamp;
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    }

    private Integer durationOf(Map<String, String> headers) {
        String rawDuration = firstNonBlank(valueOf(headers, HEADER_VARIABLE_DURATION), valueOf(headers, HEADER_DURATION_FALLBACK));
        if (rawDuration == null) {
            return null;
        }
        return Integer.parseInt(rawDuration);
    }

    private Integer callTypeOf(Map<String, String> headers) {
        return isInbound(headers) ? 1 : 2;
    }

    private boolean isInbound(Map<String, String> headers) {
        return "inbound".equalsIgnoreCase(valueOf(headers, CALL_DIRECTION));
    }

    private String valueOf(Map<String, String> headers, String key) {
        String value = headers.get(key);
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private BusinessException wrapAsBusinessException(String action, RuntimeException ex) {
        String message = ex == null || ex.getMessage() == null || ex.getMessage().isBlank()
                ? action
                : action + ": " + ex.getMessage();
        return new BusinessException(ErrorCode.INTERNAL_ERROR, message);
    }

    private final class ConnectionListener implements ServerConnectionListener {

        @Override
        public void onOpened(ServerOption serverOption) {
            connectState = serverOption.state();
            log.info("FreeSWITCH连接成功，地址={}", serverOption.addr());
        }

        @Override
        public void onClosed(ServerOption serverOption) {
            connectState = serverOption.state();
            log.warn("FreeSWITCH连接关闭，地址={}", serverOption.addr());
        }
    }

    private final class InboundEslEventListener implements IEslEventListener {

        @Override
        public void eventReceived(String address, EslEvent event) {
            handleInboundEvent(event == null ? Map.of() : event.getEventHeaders());
        }

        @Override
        public void backgroundJobResultReceived(String address, EslEvent event) {
            log.debug("收到FreeSWITCH后台任务结果，地址={}", address);
        }
    }

    @FunctionalInterface
    interface InboundClientFactory {
        InboundClient create(InboundClientOption option);
    }

    @FunctionalInterface
    interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }
}
