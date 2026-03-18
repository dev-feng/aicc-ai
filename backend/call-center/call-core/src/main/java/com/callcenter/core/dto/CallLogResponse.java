package com.callcenter.core.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 通话日志响应对象。
 */
public record CallLogResponse(
        String callId,
        String direction,
        String caller,
        String callee,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
        LocalDateTime startTime,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
        LocalDateTime endTime,
        Integer durationSec,
        String hangupCause
) {
}
