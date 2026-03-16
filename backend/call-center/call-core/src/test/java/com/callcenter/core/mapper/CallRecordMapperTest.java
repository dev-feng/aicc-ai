package com.callcenter.core.mapper;

import com.callcenter.core.entity.CallRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CallRecordMapper 插入与查询验证，确保 MyBatis-Plus 与 call_record 表映射正确。
 * <p>
 * 需已执行 schema_v1.sql 且 MySQL 可连接（profile=core）。
 * </p>
 */
@SpringBootTest
@ActiveProfiles("core")
@Transactional
class CallRecordMapperTest {

    @Autowired
    private CallRecordMapper callRecordMapper;

    @Test
    void insert_and_select_by_id() {
        LocalDateTime now = LocalDateTime.now();
        CallRecord record = CallRecord.builder()
                .callId("test-uuid-" + System.currentTimeMillis())
                .caller("1000")
                .callee("1001")
                .callType(2)
                .startTime(now)
                .endTime(now.plusSeconds(10))
                .status("hungup")
                .callDuration(10)
                .hangupCause("NORMAL_CLEARING")
                .build();

        int rows = callRecordMapper.insert(record);
        assertThat(rows).isEqualTo(1);
        assertThat(record.getId()).isNotNull();

        CallRecord found = callRecordMapper.selectById(record.getId());
        assertThat(found).isNotNull();
        assertThat(found.getCallId()).isEqualTo(record.getCallId());
        assertThat(found.getCaller()).isEqualTo("1000");
        assertThat(found.getHangupCause()).isEqualTo("NORMAL_CLEARING");
    }
}
