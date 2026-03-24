package com.callcenter.core.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 创建坐席请求。
 */
public class AgentCreateRequest {

    @NotBlank(message = "agentCode不能为空")
    private String agentCode;

    @NotBlank(message = "agentName不能为空")
    private String agentName;

    public String getAgentCode() {
        return agentCode;
    }

    public void setAgentCode(String agentCode) {
        this.agentCode = agentCode;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }
}
