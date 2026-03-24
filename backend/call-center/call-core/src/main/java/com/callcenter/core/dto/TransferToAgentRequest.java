package com.callcenter.core.dto;

import jakarta.validation.constraints.NotBlank;

public class TransferToAgentRequest {

    @NotBlank(message = "callId不能为空")
    private String callId;

    private Long targetAgentId;

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public Long getTargetAgentId() {
        return targetAgentId;
    }

    public void setTargetAgentId(Long targetAgentId) {
        this.targetAgentId = targetAgentId;
    }
}
