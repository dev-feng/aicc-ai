package com.callcenter.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 坐席绑定或解绑分机请求。
 */
public class AgentBindExtensionRequest {

    @NotNull(message = "agentId不能为空")
    private Long agentId;

    @NotBlank(message = "extensionNo不能为空")
    private String extensionNo;

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

    public String getExtensionNo() {
        return extensionNo;
    }

    public void setExtensionNo(String extensionNo) {
        this.extensionNo = extensionNo;
    }
}
