package com.callcenter.core.service.impl;

import com.callcenter.common.event.EventPublisher;
import com.callcenter.common.exception.BusinessException;
import com.callcenter.core.config.CoreProperties;
import com.callcenter.core.config.FreeSwitchConfig;
import com.callcenter.core.event.CallCreatedEvent;
import com.callcenter.core.event.CallEndedEvent;
import com.callcenter.core.service.FreeSwitchConnectionStatus;
import link.thingscloud.freeswitch.esl.InboundClient;
import link.thingscloud.freeswitch.esl.inbound.option.InboundClientOption;
import link.thingscloud.freeswitch.esl.transport.CommandResponse;
import link.thingscloud.freeswitch.esl.transport.SendEvent;
import link.thingscloud.freeswitch.esl.transport.SendMsg;
import link.thingscloud.freeswitch.esl.transport.message.EslMessage;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FreeSwitchServiceImplTest {

    @Test
    void send_command_retries_until_connection_succeeds() {
        RecordingEventPublisher eventPublisher = new RecordingEventPublisher();
        AtomicInteger attempts = new AtomicInteger();
        FreeSwitchServiceImpl service = new FreeSwitchServiceImpl(
                mockEnabledCoreProperties(),
                validConfig(),
                eventPublisher,
                option -> new FakeInboundClient(attempts.incrementAndGet() < 3),
                millis -> {
                }
        );

        String result = service.sendCommand("status", "");
        FreeSwitchConnectionStatus status = service.getStatus();

        assertThat(result).contains("EslMessage");
        assertThat(status.connected()).isTrue();
        assertThat(status.lastConnectAttempts()).isEqualTo(3);
        assertThat(status.degraded()).isFalse();
    }

    @Test
    void send_command_throws_business_exception_when_all_retries_fail() {
        RecordingEventPublisher eventPublisher = new RecordingEventPublisher();
        FreeSwitchServiceImpl service = new FreeSwitchServiceImpl(
                mockEnabledCoreProperties(),
                validConfig(),
                eventPublisher,
                option -> new FakeInboundClient(true),
                millis -> {
                }
        );

        assertThatThrownBy(() -> service.sendCommand("status", ""))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Failed to connect to FreeSWITCH");
    }

    @Test
    void initialize_allows_degraded_startup_when_configuration_is_missing() {
        FreeSwitchConfig config = new FreeSwitchConfig();
        CoreProperties coreProperties = mockEnabledCoreProperties();
        RecordingEventPublisher eventPublisher = new RecordingEventPublisher();
        FreeSwitchServiceImpl service = new FreeSwitchServiceImpl(
                coreProperties,
                config,
                eventPublisher,
                option -> new FakeInboundClient(false),
                millis -> {
                }
        );

        service.initialize();

        FreeSwitchConnectionStatus status = service.getStatus();
        assertThat(status.degraded()).isTrue();
        assertThat(status.configured()).isFalse();
        assertThat(status.connected()).isFalse();
    }

    @Test
    void handle_inbound_event_publishes_created_and_ended_events() {
        RecordingEventPublisher eventPublisher = new RecordingEventPublisher();
        FreeSwitchServiceImpl service = new FreeSwitchServiceImpl(
                mockEnabledCoreProperties(),
                validConfig(),
                eventPublisher,
                option -> new FakeInboundClient(false),
                millis -> {
                }
        );

        service.handleInboundEvent(inboundHeaders("CHANNEL_CREATE"));
        service.handleInboundEvent(inboundHeaders("CHANNEL_HANGUP_COMPLETE"));

        assertThat(eventPublisher.events).hasSize(2);
        assertThat(eventPublisher.events.get(0)).isInstanceOf(CallCreatedEvent.class);
        assertThat(eventPublisher.events.get(1)).isInstanceOf(CallEndedEvent.class);

        CallCreatedEvent createdEvent = (CallCreatedEvent) eventPublisher.events.get(0);
        assertThat(createdEvent.callId()).isEqualTo("call-uuid-1");
        assertThat(createdEvent.caller()).isEqualTo("10086");
        assertThat(createdEvent.callee()).isEqualTo("10010");
        assertThat(createdEvent.createdAt()).isEqualTo(LocalDateTime.of(2026, 3, 17, 18, 0));

        CallEndedEvent endedEvent = (CallEndedEvent) eventPublisher.events.get(1);
        assertThat(endedEvent.callId()).isEqualTo("call-uuid-1");
        assertThat(endedEvent.hangupCause()).isEqualTo("NORMAL_CLEARING");
        assertThat(endedEvent.endedAt()).isEqualTo(LocalDateTime.of(2026, 3, 17, 18, 0));
    }

    @Test
    void handle_inbound_event_tolerates_missing_fields() {
        RecordingEventPublisher eventPublisher = new RecordingEventPublisher();
        FreeSwitchServiceImpl service = new FreeSwitchServiceImpl(
                mockEnabledCoreProperties(),
                validConfig(),
                eventPublisher,
                option -> new FakeInboundClient(false),
                millis -> {
                }
        );
        Map<String, String> headers = new HashMap<>();
        headers.put("Event-Name", "CHANNEL_HANGUP_COMPLETE");
        headers.put("Call-Direction", "inbound");
        headers.put("Unique-ID", "unique-id-2");

        service.handleInboundEvent(headers);

        assertThat(eventPublisher.events).hasSize(1);
        assertThat(eventPublisher.events.get(0)).isInstanceOf(CallEndedEvent.class);
        CallEndedEvent endedEvent = (CallEndedEvent) eventPublisher.events.get(0);
        assertThat(endedEvent.callId()).isEqualTo("unique-id-2");
        assertThat(endedEvent.caller()).isNull();
        assertThat(endedEvent.callee()).isNull();
        assertThat(endedEvent.endedAt()).isNull();
    }

    private static CoreProperties mockEnabledCoreProperties() {
        CoreProperties coreProperties = new CoreProperties();
        coreProperties.setMockFsEnabled(true);
        return coreProperties;
    }

    private static FreeSwitchConfig validConfig() {
        FreeSwitchConfig config = new FreeSwitchConfig();
        config.setHost("127.0.0.1");
        config.setPort(8021);
        config.setPassword("ClueCon");
        config.setMaxRetryAttempts(3);
        config.setRetryIntervalMillis(0L);
        return config;
    }

    private static Map<String, String> inboundHeaders(String eventName) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Event-Name", eventName);
        headers.put("Call-Direction", "inbound");
        headers.put("Channel-Call-UUID", "call-uuid-1");
        headers.put("Caller-Caller-ID-Number", "10086");
        headers.put("Caller-Destination-Number", "10010");
        headers.put("Event-Date-Timestamp", "1773741600000000");
        headers.put("variable_hangup_cause", "NORMAL_CLEARING");
        return headers;
    }

    private static final class RecordingEventPublisher implements EventPublisher {

        private final List<Object> events = new ArrayList<>();

        @Override
        public void publish(Object event) {
            events.add(event);
        }
    }

    private static final class FakeInboundClient implements InboundClient {

        private final boolean failOnStart;

        private FakeInboundClient(boolean failOnStart) {
            this.failOnStart = failOnStart;
        }

        @Override
        public void start() {
            if (failOnStart) {
                throw new IllegalStateException("mock connect failure");
            }
        }

        @Override
        public void shutdown() {
        }

        @Override
        public InboundClientOption option() {
            return new InboundClientOption();
        }

        @Override
        public EslMessage sendSyncApiCommand(String s, String s1, String s2) {
            return new EslMessage();
        }

        @Override
        public EslMessage sendSyncApiCommand(String s, String s1, String s2, long l) {
            return new EslMessage();
        }

        @Override
        public void sendSyncApiCommand(String s, String s1, String s2, Consumer<EslMessage> consumer) {
            consumer.accept(new EslMessage());
        }

        @Override
        public String sendAsyncApiCommand(String s, String s1, String s2) {
            return "job-1";
        }

        @Override
        public void sendAsyncApiCommand(String s, String s1, String s2, Consumer<String> consumer) {
            consumer.accept("job-1");
        }

        @Override
        public CommandResponse setEventSubscriptions(String s, String s1, String s2) {
            return new CommandResponse("event", new EslMessage());
        }

        @Override
        public CommandResponse cancelEventSubscriptions(String s) {
            return new CommandResponse("noevents", new EslMessage());
        }

        @Override
        public CommandResponse addEventFilter(String s, String s1, String s2) {
            return new CommandResponse("filter", new EslMessage());
        }

        @Override
        public CommandResponse deleteEventFilter(String s, String s1, String s2) {
            return new CommandResponse("filter", new EslMessage());
        }

        @Override
        public CommandResponse sendEvent(String s, SendEvent sendEvent) {
            return new CommandResponse("sendevent", new EslMessage());
        }

        @Override
        public void sendEvent(String s, SendEvent sendEvent, Consumer<CommandResponse> consumer) {
            consumer.accept(new CommandResponse("sendevent", new EslMessage()));
        }

        @Override
        public CommandResponse sendMessage(String s, SendMsg sendMsg) {
            return new CommandResponse("sendmsg", new EslMessage());
        }

        @Override
        public void sendMessage(String s, SendMsg sendMsg, Consumer<CommandResponse> consumer) {
            consumer.accept(new CommandResponse("sendmsg", new EslMessage()));
        }

        @Override
        public CommandResponse setLoggingLevel(String s, String s1) {
            return new CommandResponse("log", new EslMessage());
        }

        @Override
        public CommandResponse cancelLogging(String s) {
            return new CommandResponse("nolog", new EslMessage());
        }

        @Override
        public CommandResponse close(String s) {
            return new CommandResponse("exit", new EslMessage());
        }

        @Override
        public InboundClient closeChannel(String s) {
            return this;
        }
    }
}
