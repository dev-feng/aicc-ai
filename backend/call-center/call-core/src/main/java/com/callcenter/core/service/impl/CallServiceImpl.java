package com.callcenter.core.service.impl;

import com.callcenter.common.event.EventPublisher;
import com.callcenter.core.event.CallCreatedEvent;
import com.callcenter.core.service.CallService;
import com.callcenter.core.service.FreeSwitchService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 外呼业务实现。
 */
@Service
public class CallServiceImpl implements CallService {

    private final FreeSwitchService freeSwitchService;
    private final EventPublisher eventPublisher;

    public CallServiceImpl(FreeSwitchService freeSwitchService, EventPublisher eventPublisher) {
        this.freeSwitchService = freeSwitchService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public String outbound(String caller, String callee) {
        String callId = freeSwitchService.originate(caller, callee);
        eventPublisher.publish(new CallCreatedEvent(callId, caller, callee, LocalDateTime.now(), 2));
        return callId;
    }
}
