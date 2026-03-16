package com.callcenter.core.service.impl;

import com.callcenter.common.event.EventPublisher;
import com.callcenter.core.event.CallCreatedEvent;
import com.callcenter.core.service.FreeSwitchService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CallServiceImplTest {

    @Test
    void outbound_publishes_call_created_event() {
        FreeSwitchService freeSwitchService = mock(FreeSwitchService.class);
        EventPublisher eventPublisher = mock(EventPublisher.class);
        CallServiceImpl callService = new CallServiceImpl(freeSwitchService, eventPublisher);
        when(freeSwitchService.originate("1000", "1001")).thenReturn("job-123");

        String callId = callService.outbound("1000", "1001");

        assertThat(callId).isEqualTo("job-123");
        verify(eventPublisher).publish(org.mockito.ArgumentMatchers.argThat(event ->
                event instanceof CallCreatedEvent createdEvent
                        && createdEvent.callId().equals("job-123")
                        && createdEvent.caller().equals("1000")
                        && createdEvent.callee().equals("1001")));
    }
}
