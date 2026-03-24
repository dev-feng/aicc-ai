package com.callcenter.core.ai.model;

public record AsrResult(
        String transcript,
        double confidence,
        boolean mock
) {
}
