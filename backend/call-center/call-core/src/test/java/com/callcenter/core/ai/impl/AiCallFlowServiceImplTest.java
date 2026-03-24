package com.callcenter.core.ai.impl;

import com.callcenter.common.exception.BusinessException;
import com.callcenter.core.ai.model.AiCallFlowResult;
import com.callcenter.core.event.CallCreatedEvent;
import com.callcenter.core.model.CallSession;
import com.callcenter.core.service.impl.CallSessionServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiCallFlowServiceImplTest {

    @Test
    void process_text_completes_mock_ai_flow() {
        CallSessionServiceImpl sessionService = new CallSessionServiceImpl();
        sessionService.rememberCallCreated(new CallCreatedEvent(
                "call-1",
                "1001",
                "1002",
                LocalDateTime.of(2026, 3, 24, 16, 30),
                1
        ));
        AiCallFlowServiceImpl service = new AiCallFlowServiceImpl(
                sessionService,
                new MockLlmServiceImpl(),
                new MockTtsServiceImpl()
        );

        AiCallFlowResult result = service.processText("call-1", "请帮我记录工单");

        assertThat(result.intentCode()).isEqualTo("collect_info");
        assertThat(result.transferToHuman()).isFalse();
        assertThat(result.sessionStatus()).isEqualTo(CallSession.STATUS_COMPLETED);
        assertThat(result.mock()).isTrue();
        assertThat(sessionService.getSession("call-1").orElseThrow().getCurrentStatus())
                .isEqualTo(CallSession.STATUS_COMPLETED);
    }

    @Test
    void process_text_marks_transfer_pending_when_llm_requires_human() {
        CallSessionServiceImpl sessionService = new CallSessionServiceImpl();
        sessionService.rememberCallCreated(new CallCreatedEvent(
                "call-2",
                "1001",
                "1002",
                LocalDateTime.of(2026, 3, 24, 16, 31),
                1
        ));
        AiCallFlowServiceImpl service = new AiCallFlowServiceImpl(
                sessionService,
                new MockLlmServiceImpl(),
                new MockTtsServiceImpl()
        );

        AiCallFlowResult result = service.processText("call-2", "我要人工");

        assertThat(result.transferToHuman()).isTrue();
        assertThat(result.sessionStatus()).isEqualTo(CallSession.STATUS_TRANSFER_PENDING);
        assertThat(sessionService.getSession("call-2").orElseThrow().getCurrentStatus())
                .isEqualTo(CallSession.STATUS_TRANSFER_PENDING);
    }

    @Test
    void process_text_throws_when_session_missing() {
        AiCallFlowServiceImpl service = new AiCallFlowServiceImpl(
                new CallSessionServiceImpl(),
                new MockLlmServiceImpl(),
                new MockTtsServiceImpl()
        );

        assertThatThrownBy(() -> service.processText("missing", "测试"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("call session不存在");
    }
}
