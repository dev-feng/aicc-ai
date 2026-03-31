package com.callcenter.core.dto;

public record TransferToAgentResponse(
        String callId,
        Long agentId,
        String extensionNo,
        String status,
        boolean success
) {
}
