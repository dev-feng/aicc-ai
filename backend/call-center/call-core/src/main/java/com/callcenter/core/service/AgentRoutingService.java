package com.callcenter.core.service;

public interface AgentRoutingService {

    AgentRoutingResult transferToAgent(String callId, Long targetAgentId);

    record AgentRoutingResult(
            String callId,
            Long agentId,
            String extensionNo,
            String status,
            boolean success
    ) {
    }
}
