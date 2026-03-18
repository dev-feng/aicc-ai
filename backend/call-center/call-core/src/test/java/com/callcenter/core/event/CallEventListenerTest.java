package com.callcenter.core.event;

import com.callcenter.core.service.CallLogService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CallEventListenerTest {

    @Test
    void on_call_created_delegates_to_call_log_service() {
        CallLogService callLogService = mock(CallLogService.class);
        CallEventListener listener = new CallEventListener(callLogService);
        CallCreatedEvent event = new CallCreatedEvent(
                "call-created-1",
                "1000",
                "1001",
                LocalDateTime.of(2026, 3, 18, 16, 0, 0),
                1
        );

        listener.onCallCreated(event);

        verify(callLogService).rememberCallCreated(event);
    }

    @Test
    void on_call_ended_delegates_to_call_log_service() {
        CallLogService callLogService = mock(CallLogService.class);
        CallEventListener listener = new CallEventListener(callLogService);
        CallEndedEvent event = new CallEndedEvent(
                "call-ended-1",
                "1000",
                "1001",
                "NORMAL_CLEARING",
                LocalDateTime.of(2026, 3, 18, 16, 1, 0),
                2,
                LocalDateTime.of(2026, 3, 18, 16, 0, 0),
                LocalDateTime.of(2026, 3, 18, 16, 0, 2),
                LocalDateTime.of(2026, 3, 18, 16, 0, 5),
                60
        );

        listener.onCallEnded(event);

        verify(callLogService).recordCallEnded(event);
    }
}
