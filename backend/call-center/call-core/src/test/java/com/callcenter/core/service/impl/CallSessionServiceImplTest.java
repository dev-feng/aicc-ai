package com.callcenter.core.service.impl;

import com.callcenter.common.exception.BusinessException;
import com.callcenter.core.event.CallCreatedEvent;
import com.callcenter.core.event.CallEndedEvent;
import com.callcenter.core.model.CallSession;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CallSessionServiceImplTest {

    @Test
    void remember_call_created_stores_active_session() {
        CallSessionServiceImpl service = new CallSessionServiceImpl();
        CallCreatedEvent event = new CallCreatedEvent("call-1", "1001", "1002", LocalDateTime.of(2026, 3, 24, 16, 0), 1);

        service.rememberCallCreated(event);

        CallSession session = service.getSession("call-1").orElseThrow();
        assertThat(session.getCurrentStatus()).isEqualTo(CallSession.STATUS_RINGING);
        assertThat(session.isActive()).isTrue();
        assertThat(session.getCaller()).isEqualTo("1001");
    }

    @Test
    void remember_call_ended_completes_existing_session() {
        CallSessionServiceImpl service = new CallSessionServiceImpl();
        service.rememberCallCreated(new CallCreatedEvent("call-1", "1001", "1002", LocalDateTime.of(2026, 3, 24, 16, 0), 1));
        CallEndedEvent event = new CallEndedEvent(
                "call-1",
                "1001",
                "1002",
                "NORMAL_CLEARING",
                LocalDateTime.of(2026, 3, 24, 16, 3),
                1,
                LocalDateTime.of(2026, 3, 24, 16, 0),
                LocalDateTime.of(2026, 3, 24, 16, 0, 2),
                LocalDateTime.of(2026, 3, 24, 16, 0, 5),
                180
        );

        service.rememberCallEnded(event);

        CallSession session = service.getSession("call-1").orElseThrow();
        assertThat(session.getCurrentStatus()).isEqualTo(CallSession.STATUS_COMPLETED);
        assertThat(session.isActive()).isFalse();
        assertThat(session.getEndTime()).isEqualTo(LocalDateTime.of(2026, 3, 24, 16, 3));
        assertThat(session.getAnswerTime()).isEqualTo(LocalDateTime.of(2026, 3, 24, 16, 0, 5));
    }

    @Test
    void remember_call_ended_can_build_session_without_created_event() {
        CallSessionServiceImpl service = new CallSessionServiceImpl();
        CallEndedEvent event = new CallEndedEvent(
                "call-2",
                "1003",
                "1004",
                "NO_ANSWER",
                LocalDateTime.of(2026, 3, 24, 16, 5),
                2,
                LocalDateTime.of(2026, 3, 24, 16, 4),
                LocalDateTime.of(2026, 3, 24, 16, 4, 1),
                null,
                60
        );

        service.rememberCallEnded(event);

        CallSession session = service.getSession("call-2").orElseThrow();
        assertThat(session.getCurrentStatus()).isEqualTo(CallSession.STATUS_COMPLETED);
        assertThat(session.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 3, 24, 16, 4));
        assertThat(session.getHangupCause()).isEqualTo("NO_ANSWER");
    }

    @Test
    void update_session_status_changes_active_flag() {
        CallSessionServiceImpl service = new CallSessionServiceImpl();
        service.rememberCallCreated(new CallCreatedEvent("call-3", "1005", "1006", LocalDateTime.of(2026, 3, 24, 16, 10), 1));

        CallSession session = service.updateSessionStatus("call-3", CallSession.STATUS_TRANSFER_PENDING);

        assertThat(session.getCurrentStatus()).isEqualTo(CallSession.STATUS_TRANSFER_PENDING);
        assertThat(session.isActive()).isTrue();
    }

    @Test
    void update_session_status_throws_when_missing() {
        CallSessionServiceImpl service = new CallSessionServiceImpl();

        assertThatThrownBy(() -> service.updateSessionStatus("missing", CallSession.STATUS_TRANSFER_PENDING))
                .isInstanceOf(BusinessException.class)
                .hasMessage("call session不存在");
    }
}
