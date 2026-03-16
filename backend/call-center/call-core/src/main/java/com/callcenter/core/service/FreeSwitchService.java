package com.callcenter.core.service;

/**
 * FreeSWITCH ESL 基础能力抽象。
 */
public interface FreeSwitchService {

    /**
     * 发起一通外呼并返回后台任务标识。
     *
     * @param caller caller number
     * @param callee callee number
     * @return background job id
     */
    String originate(String caller, String callee);

    /**
     * 发送同步命令。
     *
     * @param command command name
     * @param arguments command arguments
     * @return command response text
     */
    String sendCommand(String command, String arguments);

    /**
     * 订阅指定 FreeSWITCH 事件。
     *
     * @param events event names
     */
    void subscribeEvents(String... events);

    /**
     * 获取当前连接状态。
     *
     * @return connection status
     */
    FreeSwitchConnectionStatus getStatus();
}
