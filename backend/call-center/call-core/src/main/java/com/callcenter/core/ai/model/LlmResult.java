package com.callcenter.core.ai.model;

public record LlmResult(
        String replyText,
        String intentCode,
        boolean transferToHuman,
        boolean mock
) {
}
