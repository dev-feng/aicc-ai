package com.callcenter.core.dto;

/**
 * 外呼响应参数。
 */
public class OutboundCallResponse {

    private final String callId;

    public OutboundCallResponse(String callId) {
        this.callId = callId;
    }

    /**
     * 获取外呼追踪标识。
     *
     * @return call id
     */
    public String getCallId() {
        return callId;
    }
}
