package com.callcenter.core.service.impl;

import com.callcenter.common.exception.BusinessException;
import com.callcenter.common.exception.ErrorCode;
import com.callcenter.core.config.CoreProperties;
import com.callcenter.core.config.FreeSwitchConfig;
import com.callcenter.core.service.FreeSwitchConnectionStatus;
import com.callcenter.core.service.FreeSwitchService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import link.thingscloud.freeswitch.esl.InboundClient;
import link.thingscloud.freeswitch.esl.ServerConnectionListener;
import link.thingscloud.freeswitch.esl.inbound.option.ConnectState;
import link.thingscloud.freeswitch.esl.inbound.option.InboundClientOption;
import link.thingscloud.freeswitch.esl.inbound.option.ServerOption;
import link.thingscloud.freeswitch.esl.transport.CommandResponse;
import link.thingscloud.freeswitch.esl.transport.message.EslMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    private final CoreProperties coreProperties;
    private final FreeSwitchConfig freeSwitchConfig;
    private final InboundClientFactory inboundClientFactory;
    private final Sleeper sleeper;
    private final Set<String> subscribedEvents = new CopyOnWriteArraySet<>();
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
    public FreeSwitchServiceImpl(CoreProperties coreProperties, FreeSwitchConfig freeSwitchConfig) {
        this(coreProperties, freeSwitchConfig, InboundClient::newInstance, Thread::sleep);
    }

    FreeSwitchServiceImpl(
            CoreProperties coreProperties,
            FreeSwitchConfig freeSwitchConfig,
            InboundClientFactory inboundClientFactory,
            Sleeper sleeper
    ) {
        this.coreProperties = coreProperties;
        this.freeSwitchConfig = freeSwitchConfig;
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
            lastErrorMessage = "FreeSWITCH integration is disabled by configuration.";
            log.warn("FreeSWITCH integration is disabled.");
            return;
        }
        if (!freeSwitchConfig.hasRequiredConnectionInfo()) {
            degraded = true;
            lastErrorMessage = "Missing FreeSWITCH host/port/password configuration.";
            log.warn("FreeSWITCH configuration is incomplete. Application starts in degraded mode.");
            return;
        }
        try {
            ensureConnected(false);
        } catch (BusinessException ex) {
            if (coreProperties.isMockFsEnabled()) {
                degraded = true;
                lastErrorMessage = ex.getMessage();
                log.warn("FreeSWITCH startup check failed, continue in degraded mode: {}", ex.getMessage());
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
                log.warn("FreeSWITCH client shutdown failed: {}", ex.getMessage());
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
                log.warn("FreeSWITCH connection attempt {}/{} failed: {}",
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
            log.debug("Ignore FreeSWITCH client shutdown error after failed connect: {}", ex.getMessage());
        }
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
            log.info("FreeSWITCH connection opened: {}", serverOption.addr());
        }

        @Override
        public void onClosed(ServerOption serverOption) {
            connectState = serverOption.state();
            log.warn("FreeSWITCH connection closed: {}", serverOption.addr());
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
