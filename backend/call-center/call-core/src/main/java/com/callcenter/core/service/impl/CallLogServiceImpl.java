package com.callcenter.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.callcenter.core.dto.CallLogResponse;
import com.callcenter.core.entity.CallRecord;
import com.callcenter.core.event.CallCreatedEvent;
import com.callcenter.core.event.CallEndedEvent;
import com.callcenter.core.mapper.CallRecordMapper;
import com.callcenter.core.service.CallLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 基于 Mapper 的通话日志落库实现。
 */
@Service
public class CallLogServiceImpl implements CallLogService {

    private static final Logger log = LoggerFactory.getLogger(CallLogServiceImpl.class);

    private final CallRecordMapper callRecordMapper;
    private final Map<String, CallCreatedEvent> createdEvents = new ConcurrentHashMap<>();

    public CallLogServiceImpl(CallRecordMapper callRecordMapper) {
        this.callRecordMapper = callRecordMapper;
    }

    @Override
    public void rememberCallCreated(CallCreatedEvent event) {
        if (event == null || event.callId() == null || event.callId().isBlank()) {
            return;
        }
        createdEvents.put(event.callId(), event);
    }

    @Override
    public void recordCallEnded(CallEndedEvent event) {
        if (event == null || event.callId() == null || event.callId().isBlank()) {
            log.warn("忽略缺少callId的CallEndedEvent。");
            return;
        }

        CallCreatedEvent createdEvent = createdEvents.remove(event.callId());
        CallRecord record = buildRecord(event, createdEvent);
        if (record == null) {
            log.warn("通话记录字段不完整，忽略写库，callId={}", event.callId());
            return;
        }

        try {
            callRecordMapper.insert(record);
        } catch (DuplicateKeyException ex) {
            log.warn("通话记录已存在，忽略重复写入，callId={}", event.callId());
        }
    }

    @Override
    public List<CallLogResponse> queryLogs(String phone, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<CallRecord> queryWrapper = new LambdaQueryWrapper<CallRecord>()
                .and(wrapper -> wrapper.eq(CallRecord::getCaller, phone).or().eq(CallRecord::getCallee, phone))
                .ge(startTime != null, CallRecord::getStartTime, startTime)
                .le(endTime != null, CallRecord::getStartTime, endTime)
                .orderByDesc(CallRecord::getStartTime);

        return callRecordMapper.selectList(queryWrapper).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private CallRecord buildRecord(CallEndedEvent event, CallCreatedEvent createdEvent) {
        String caller = firstNonBlank(event.caller(), createdEvent == null ? null : createdEvent.caller());
        String callee = firstNonBlank(event.callee(), createdEvent == null ? null : createdEvent.callee());
        Integer callType = event.callType() != null ? event.callType() : createdEvent == null ? null : createdEvent.callType();
        LocalDateTime startTime = firstNonNull(event.startTime(), createdEvent == null ? null : createdEvent.createdAt(), event.endedAt());
        LocalDateTime ringingTime = firstNonNull(event.ringingTime(), startTime);
        LocalDateTime answerTime = event.answerTime();
        LocalDateTime endTime = event.endedAt();

        if (caller == null || callee == null || callType == null || startTime == null || endTime == null) {
            return null;
        }

        int callDuration = event.callDuration() != null
                ? Math.max(event.callDuration(), 0)
                : secondsBetween(startTime, endTime);
        int ringingDuration = answerTime != null
                ? secondsBetween(ringingTime, answerTime)
                : callDuration;
        int answerDuration = secondsBetween(answerTime, endTime);

        return CallRecord.builder()
                .callId(event.callId())
                .caller(caller)
                .callee(callee)
                .callType(callType)
                .startTime(startTime)
                .ringingTime(ringingTime)
                .answerTime(answerTime)
                .endTime(endTime)
                .status("hungup")
                .ringingDuration(ringingDuration)
                .answerDuration(answerDuration)
                .callDuration(callDuration)
                .hangupCause(event.hangupCause())
                .build();
    }

    private int secondsBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return (int) Math.max(Duration.between(start, end).getSeconds(), 0L);
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private CallLogResponse toResponse(CallRecord record) {
        return new CallLogResponse(
                record.getCallId(),
                record.getCallType() != null && record.getCallType() == 1 ? "inbound" : "outbound",
                record.getCaller(),
                record.getCallee(),
                record.getStartTime(),
                record.getEndTime(),
                record.getCallDuration(),
                record.getHangupCause()
        );
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
}
