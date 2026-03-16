package com.callcenter.core.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 记录关键呼叫事件，便于阶段验收。
 */
@Component
public class CallEventLogListener {

    private static final Logger log = LoggerFactory.getLogger(CallEventLogListener.class);

    @EventListener
    public void onCallCreated(CallCreatedEvent event) {
        log.info("CallCreatedEvent published, callId={}, caller={}, callee={}",
                event.callId(), event.caller(), event.callee());
    }

    @EventListener
    public void onCallEnded(CallEndedEvent event) {
        log.info("CallEndedEvent published, callId={}, caller={}, callee={}, hangupCause={}",
                event.callId(), event.caller(), event.callee(), event.hangupCause());
    }
}
