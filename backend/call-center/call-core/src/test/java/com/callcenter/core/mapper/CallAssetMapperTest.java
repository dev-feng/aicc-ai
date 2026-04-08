package com.callcenter.core.mapper;

import com.callcenter.core.entity.CallRecording;
import com.callcenter.core.entity.CallSummary;
import com.callcenter.core.entity.TransferResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CallAssetMapperTest {

    @Autowired
    private CallRecordingMapper callRecordingMapper;

    @Autowired
    private CallSummaryMapper callSummaryMapper;

    @Autowired
    private TransferResultMapper transferResultMapper;

    @Test
    void insert_phase3_assets_successfully() {
        String callId = "phase3-call-" + System.currentTimeMillis();

        CallRecording recording = CallRecording.builder()
                .callId(callId)
                .recordingPath("/recordings/" + callId + ".wav")
                .resourceId("fs-rec-" + callId)
                .format("wav")
                .durationSec(35)
                .status("success")
                .sourceType("stub")
                .build();
        assertThat(callRecordingMapper.insert(recording)).isEqualTo(1);
        assertThat(recording.getId()).isNotNull();

        CallSummary summary = CallSummary.builder()
                .callId(callId)
                .summaryText("客户咨询套餐变更，系统给出处理建议。")
                .summaryStatus("success")
                .resultCode("handled_by_ai")
                .tags("billing,change-plan")
                .sourceType("stub")
                .build();
        assertThat(callSummaryMapper.insert(summary)).isEqualTo(1);
        assertThat(summary.getId()).isNotNull();

        TransferResult transferResult = TransferResult.builder()
                .callId(callId)
                .attemptNo(1)
                .targetAgentId(12L)
                .targetExtensionNo("8001")
                .transferStatus("failed")
                .failureReason("no_agent_available")
                .fallbackAction("play_busy_message")
                .requestedAt(LocalDateTime.of(2026, 3, 31, 16, 0, 0))
                .completedAt(LocalDateTime.of(2026, 3, 31, 16, 0, 8))
                .build();
        assertThat(transferResultMapper.insert(transferResult)).isEqualTo(1);
        assertThat(transferResult.getId()).isNotNull();

        CallRecording storedRecording = callRecordingMapper.selectById(recording.getId());
        CallSummary storedSummary = callSummaryMapper.selectById(summary.getId());
        TransferResult storedTransferResult = transferResultMapper.selectById(transferResult.getId());

        assertThat(storedRecording.getCallId()).isEqualTo(callId);
        assertThat(storedRecording.getDurationSec()).isEqualTo(35);
        assertThat(storedSummary.getSummaryText()).contains("套餐变更");
        assertThat(storedSummary.getSummaryStatus()).isEqualTo("success");
        assertThat(storedTransferResult.getTransferStatus()).isEqualTo("failed");
        assertThat(storedTransferResult.getFailureReason()).isEqualTo("no_agent_available");
    }
}
