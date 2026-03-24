package com.callcenter.core.dto;

import java.util.List;

/**
 * 坐席响应对象。
 */
public record AgentResponse(
        Long agentId,
        String agentCode,
        String agentName,
        String status,
        Integer enabled,
        List<String> extensionNos
) {
}
