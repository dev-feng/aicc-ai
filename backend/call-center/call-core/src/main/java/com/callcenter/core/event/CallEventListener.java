package com.callcenter.core.event;

import com.callcenter.core.service.CallLogService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 监听通话领域事件并驱动落库。
 */
@Component
public class CallEventListener {

    private final CallLogService callLogService;

    public CallEventListener(CallLogService callLogService) {
        this.callLogService = callLogService;
    }

    @EventListener
    public void onCallCreated(CallCreatedEvent event) {
        callLogService.rememberCallCreated(event);
    }

    @EventListener
    public void onCallEnded(CallEndedEvent event) {
        callLogService.recordCallEnded(event);
    }
}
