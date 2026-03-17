package com.callcenter.core.dto;

import java.time.LocalDateTime;

/**
 * 通话日志响应对象。
 */
public record CallLogResponse(
        String callId,
        String direction,
        String caller,
        String callee,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer durationSec,
        String hangupCause
) {
}
