package com.callcenter.core.ai.model;

public record TtsResult(
        String text,
        byte[] audioPayload,
        String audioFormat,
        boolean mock
) {
}
