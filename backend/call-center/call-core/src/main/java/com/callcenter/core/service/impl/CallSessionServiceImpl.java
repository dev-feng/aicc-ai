package com.callcenter.core.service.impl;

import com.callcenter.common.exception.BusinessException;
import com.callcenter.common.exception.ErrorCode;
import com.callcenter.core.event.CallCreatedEvent;
import com.callcenter.core.event.CallEndedEvent;
import com.callcenter.core.model.CallSession;
import com.callcenter.core.service.CallSessionService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class CallSessionServiceImpl implements CallSessionService {

    private final ConcurrentMap<String, CallSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void rememberCallCreated(CallCreatedEvent event) {
        if (event == null || isBlank(event.callId())) {
            return;
        }
        sessions.compute(event.callId(), (callId, existing) -> {
            LocalDateTime now = nowOf(event.createdAt());
            CallSession session = existing != null ? existing : new CallSession();
            session.setCallId(callId);
            session.setCaller(firstNonBlank(event.caller(), session.getCaller()));
            session.setCallee(firstNonBlank(event.callee(), session.getCallee()));
            session.setCallType(event.callType() != null ? event.callType() : session.getCallType());
            session.setCreatedAt(session.getCreatedAt() != null ? session.getCreatedAt() : now);
            session.setRingingTime(session.getRingingTime() != null ? session.getRingingTime() : now);
            session.setCurrentStatus(CallSession.STATUS_RINGING);
            session.setActive(true);
            session.setUpdatedAt(now);
            return session;
        });
    }

    @Override
    public void rememberCallEnded(CallEndedEvent event) {
        if (event == null || isBlank(event.callId())) {
            return;
        }
        sessions.compute(event.callId(), (callId, existing) -> {
            LocalDateTime endTime = nowOf(event.endedAt());
            CallSession session = existing != null ? existing : new CallSession();
            session.setCallId(callId);
            session.setCaller(firstNonBlank(event.caller(), session.getCaller()));
            session.setCallee(firstNonBlank(event.callee(), session.getCallee()));
            session.setCallType(event.callType() != null ? event.callType() : session.getCallType());
            session.setCreatedAt(firstNonNull(session.getCreatedAt(), event.startTime(), endTime));
            session.setRingingTime(firstNonNull(session.getRingingTime(), event.ringingTime(), session.getCreatedAt()));
            session.setAnswerTime(firstNonNull(event.answerTime(), session.getAnswerTime()));
            session.setEndTime(endTime);
            session.setHangupCause(firstNonBlank(event.hangupCause(), session.getHangupCause()));
            session.setCurrentStatus(CallSession.STATUS_COMPLETED);
            session.setActive(false);
            session.setUpdatedAt(endTime);
            return session;
        });
    }

    @Override
    public Optional<CallSession> getSession(String callId) {
        if (isBlank(callId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessions.get(callId));
    }

    @Override
    public List<CallSession> listActiveSessions() {
        return sessions.values().stream()
                .filter(CallSession::isActive)
                .sorted(Comparator.comparing(CallSession::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    public CallSession updateSessionStatus(String callId, String currentStatus) {
        if (isBlank(callId) || isBlank(currentStatus)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "callId/currentStatus不能为空");
        }
        CallSession session = sessions.get(callId);
        if (session == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "call session不存在");
        }
        session.setCurrentStatus(currentStatus);
        session.setUpdatedAt(LocalDateTime.now());
        session.setActive(!CallSession.STATUS_COMPLETED.equals(currentStatus));
        return session;
    }

    private LocalDateTime nowOf(LocalDateTime value) {
        return value != null ? value : LocalDateTime.now();
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstNonBlank(String first, String second) {
        return !isBlank(first) ? first : second;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
