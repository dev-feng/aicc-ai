package com.callcenter.core.service;

import com.callcenter.core.event.CallCreatedEvent;
import com.callcenter.core.event.CallEndedEvent;
import com.callcenter.core.model.CallSession;

import java.util.List;
import java.util.Optional;

public interface CallSessionService {

    void rememberCallCreated(CallCreatedEvent event);

    void rememberCallEnded(CallEndedEvent event);

    Optional<CallSession> getSession(String callId);

    List<CallSession> listActiveSessions();

    CallSession updateSessionStatus(String callId, String currentStatus);
}
