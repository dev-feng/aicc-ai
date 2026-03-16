package com.callcenter.core.event;

import java.time.LocalDateTime;

/**
 * 呼叫创建事件。
 */
public record CallCreatedEvent(
        String callId,
        String caller,
        String callee,
        LocalDateTime createdAt
) {
}
