package com.callcenter.core.service.impl;

import com.callcenter.core.entity.CallRecord;
import com.callcenter.core.event.CallCreatedEvent;
import com.callcenter.core.event.CallEndedEvent;
import com.callcenter.core.mapper.CallRecordMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class CallLogServiceImplTest {

    @Test
    void record_call_ended_writes_call_record_with_computed_durations() {
        CallRecordMapper mapper = mock(CallRecordMapper.class);
        CallLogServiceImpl service = new CallLogServiceImpl(mapper);
        service.rememberCallCreated(new CallCreatedEvent(
                "call-1",
                "1000",
                "1001",
                LocalDateTime.of(2026, 3, 17, 18, 10, 0),
                2
        ));

        service.recordCallEnded(new CallEndedEvent(
                "call-1",
                null,
                null,
                "NORMAL_CLEARING",
                LocalDateTime.of(2026, 3, 17, 18, 10, 20),
                2,
                null,
                LocalDateTime.of(2026, 3, 17, 18, 10, 2),
                LocalDateTime.of(2026, 3, 17, 18, 10, 5),
                null
        ));

        ArgumentCaptor<CallRecord> captor = ArgumentCaptor.forClass(CallRecord.class);
        verify(mapper).insert(captor.capture());
        CallRecord record = captor.getValue();
        assertThat(record.getCallId()).isEqualTo("call-1");
        assertThat(record.getCaller()).isEqualTo("1000");
        assertThat(record.getCallee()).isEqualTo("1001");
        assertThat(record.getCallType()).isEqualTo(2);
        assertThat(record.getStartTime()).isEqualTo(LocalDateTime.of(2026, 3, 17, 18, 10, 0));
        assertThat(record.getRingingDuration()).isEqualTo(3);
        assertThat(record.getAnswerDuration()).isEqualTo(15);
        assertThat(record.getCallDuration()).isEqualTo(20);
        assertThat(record.getStatus()).isEqualTo("hungup");
    }

    @Test
    void record_call_ended_ignores_duplicate_key() {
        CallRecordMapper mapper = mock(CallRecordMapper.class);
        doThrow(new DuplicateKeyException("duplicate")).when(mapper).insert(org.mockito.ArgumentMatchers.any(CallRecord.class));
        CallLogServiceImpl service = new CallLogServiceImpl(mapper);

        service.recordCallEnded(new CallEndedEvent(
                "call-2",
                "1002",
                "1003",
                "NORMAL_CLEARING",
                LocalDateTime.of(2026, 3, 17, 18, 11, 0),
                1,
                LocalDateTime.of(2026, 3, 17, 18, 10, 0),
                LocalDateTime.of(2026, 3, 17, 18, 10, 0),
                null,
                60
        ));

        verify(mapper, times(1)).insert(org.mockito.ArgumentMatchers.any(CallRecord.class));
    }

    @Test
    void record_call_ended_skips_when_required_fields_are_missing() {
        CallRecordMapper mapper = mock(CallRecordMapper.class);
        CallLogServiceImpl service = new CallLogServiceImpl(mapper);

        service.recordCallEnded(new CallEndedEvent(
                "call-3",
                null,
                null,
                "CALL_REJECTED",
                LocalDateTime.of(2026, 3, 17, 18, 12, 0),
                null,
                null,
                null,
                null,
                null
        ));

        verify(mapper, times(0)).insert(org.mockito.ArgumentMatchers.any(CallRecord.class));
    }

    @Test
    void record_call_ended_uses_end_time_as_ringing_end_when_not_answered() {
        CallRecordMapper mapper = mock(CallRecordMapper.class);
        CallLogServiceImpl service = new CallLogServiceImpl(mapper);
        service.rememberCallCreated(new CallCreatedEvent(
                "call-4",
                "1002",
                "1003",
                LocalDateTime.of(2026, 3, 17, 18, 32, 55),
                2
        ));

        service.recordCallEnded(new CallEndedEvent(
                "call-4",
                "1002",
                "1003",
                "NO_ANSWER",
                LocalDateTime.of(2026, 3, 17, 18, 33, 54),
                2,
                LocalDateTime.of(2026, 3, 17, 18, 32, 55),
                LocalDateTime.of(2026, 3, 17, 18, 32, 55),
                null,
                60
        ));

        ArgumentCaptor<CallRecord> captor = ArgumentCaptor.forClass(CallRecord.class);
        verify(mapper).insert(captor.capture());
        CallRecord record = captor.getValue();
        assertThat(record.getRingingDuration()).isEqualTo(60);
        assertThat(record.getAnswerDuration()).isEqualTo(0);
        assertThat(record.getCallDuration()).isEqualTo(60);
    }
}
