package com.callcenter.core.service;

import com.callcenter.core.dto.CallLogResponse;
import com.callcenter.core.event.CallCreatedEvent;
import com.callcenter.core.event.CallEndedEvent;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通话日志写入服务。
 */
public interface CallLogService {

    /**
     * 缓存呼叫创建事件，供挂断落库时补齐字段。
     *
     * @param event 呼叫创建事件
     */
    void rememberCallCreated(CallCreatedEvent event);

    /**
     * 根据挂断事件写入通话日志。
     *
     * @param event 呼叫结束事件
     */
    void recordCallEnded(CallEndedEvent event);

    /**
     * 查询通话日志。
     *
     * @param phone 查询号码
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 日志列表
     */
    List<CallLogResponse> queryLogs(String phone, LocalDateTime startTime, LocalDateTime endTime);
}
