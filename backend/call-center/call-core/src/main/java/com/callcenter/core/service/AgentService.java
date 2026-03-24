package com.callcenter.core.service;

import com.callcenter.core.dto.AgentBindExtensionRequest;
import com.callcenter.core.dto.AgentCreateRequest;
import com.callcenter.core.dto.AgentResponse;

/**
 * 坐席管理服务。
 */
public interface AgentService {

    AgentResponse createAgent(AgentCreateRequest request);

    AgentResponse getAgent(Long agentId);

    AgentResponse bindExtension(AgentBindExtensionRequest request);

    AgentResponse unbindExtension(AgentBindExtensionRequest request);
}
