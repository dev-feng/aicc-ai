package com.callcenter.core.ai.impl;

import com.callcenter.common.exception.BusinessException;
import com.callcenter.common.exception.ErrorCode;
import com.callcenter.core.ai.AiCallFlowService;
import com.callcenter.core.ai.LlmService;
import com.callcenter.core.ai.TtsService;
import com.callcenter.core.ai.model.AiCallFlowResult;
import com.callcenter.core.ai.model.LlmResult;
import com.callcenter.core.ai.model.TtsResult;
import com.callcenter.core.model.CallSession;
import com.callcenter.core.service.AgentRoutingService;
import com.callcenter.core.service.CallSessionService;
import org.springframework.stereotype.Service;

@Service
public class AiCallFlowServiceImpl implements AiCallFlowService {

    private final CallSessionService callSessionService;
    private final LlmService llmService;
    private final TtsService ttsService;
    private final AgentRoutingService agentRoutingService;

    public AiCallFlowServiceImpl(
            CallSessionService callSessionService,
            LlmService llmService,
            TtsService ttsService,
            AgentRoutingService agentRoutingService
    ) {
        this.callSessionService = callSessionService;
        this.llmService = llmService;
        this.ttsService = ttsService;
        this.agentRoutingService = agentRoutingService;
    }

    @Override
    public AiCallFlowResult processText(String callId, String transcript) {
        if (callId == null || callId.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "callId不能为空");
        }
        if (transcript == null || transcript.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "transcript不能为空");
        }

        callSessionService.getSession(callId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "call session不存在"));

        callSessionService.updateSessionStatus(callId, CallSession.STATUS_THINKING);
        LlmResult llmResult = llmService.generateReply(callId, transcript);

        if (llmResult.transferToHuman()) {
            callSessionService.updateSessionStatus(callId, CallSession.STATUS_TRANSFER_PENDING);
            AgentRoutingService.AgentRoutingResult routingResult = agentRoutingService.transferToAgent(callId, null);
            return new AiCallFlowResult(
                    callId,
                    transcript,
                    llmResult.intentCode(),
                    llmResult.replyText(),
                    routingResult.status(),
                    true,
                    llmResult.mock()
            );
        }

        callSessionService.updateSessionStatus(callId, CallSession.STATUS_SPEAKING);
        TtsResult ttsResult = ttsService.synthesize(callId, llmResult.replyText());
        CallSession session = callSessionService.updateSessionStatus(callId, CallSession.STATUS_COMPLETED);

        return new AiCallFlowResult(
                callId,
                transcript,
                llmResult.intentCode(),
                ttsResult.text(),
                session.getCurrentStatus(),
                false,
                llmResult.mock() && ttsResult.mock()
        );
    }
}
