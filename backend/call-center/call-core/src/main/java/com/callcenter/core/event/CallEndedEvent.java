package com.callcenter.core.event;

import java.time.LocalDateTime;

/**
 * 呼叫结束事件。
 */
public record CallEndedEvent(
        String callId,
        String caller,
        String callee,
        String hangupCause,
        LocalDateTime endedAt,
        Integer callType,
        LocalDateTime startTime,
        LocalDateTime ringingTime,
        LocalDateTime answerTime,
        Integer callDuration
) {
}
