package com.callcenter.core.event;

import com.callcenter.core.service.CallLogService;
import com.callcenter.core.service.CallSessionService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 监听通话领域事件并驱动落库。
 */
@Component
public class CallEventListener {

    private final CallLogService callLogService;
    private final CallSessionService callSessionService;

    public CallEventListener(CallLogService callLogService, CallSessionService callSessionService) {
        this.callLogService = callLogService;
        this.callSessionService = callSessionService;
    }

    @EventListener
    public void onCallCreated(CallCreatedEvent event) {
        callLogService.rememberCallCreated(event);
        callSessionService.rememberCallCreated(event);
    }

    @EventListener
    public void onCallEnded(CallEndedEvent event) {
        callLogService.recordCallEnded(event);
        callSessionService.rememberCallEnded(event);
    }
}
