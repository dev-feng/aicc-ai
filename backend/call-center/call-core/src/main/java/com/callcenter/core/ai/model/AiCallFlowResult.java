package com.callcenter.core.ai.model;

public record AiCallFlowResult(
        String callId,
        String transcript,
        String intentCode,
        String replyText,
        String sessionStatus,
        boolean transferToHuman,
        boolean mock
) {
}
