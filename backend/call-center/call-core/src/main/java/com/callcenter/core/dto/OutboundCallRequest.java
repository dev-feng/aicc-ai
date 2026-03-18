package com.callcenter.core.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 外呼请求参数。
 */
public class OutboundCallRequest {

    @NotBlank(message = "caller不能为空")
    private String caller;

    @NotBlank(message = "callee不能为空")
    private String callee;

    /**
     * 获取主叫号码。
     *
     * @return caller
     */
    public String getCaller() {
        return caller;
    }

    /**
     * 设置主叫号码。
     *
     * @param caller caller
     */
    public void setCaller(String caller) {
        this.caller = caller;
    }

    /**
     * 获取被叫号码。
     *
     * @return callee
     */
    public String getCallee() {
        return callee;
    }

    /**
     * 设置被叫号码。
     *
     * @param callee callee
     */
    public void setCallee(String callee) {
        this.callee = callee;
    }
}
