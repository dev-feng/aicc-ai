package com.callcenter.core.service;

/**
 * 外呼业务服务。
 */
public interface CallService {

    /**
     * 发起一通外呼。
     *
     * @param caller caller number
     * @param callee callee number
     * @return call id
     */
    String outbound(String caller, String callee);
}
