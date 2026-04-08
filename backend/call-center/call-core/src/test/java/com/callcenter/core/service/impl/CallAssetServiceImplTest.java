package com.callcenter.core.service.impl;

import com.callcenter.core.entity.CallRecording;
import com.callcenter.core.entity.CallSummary;
import com.callcenter.core.entity.TransferResult;
import com.callcenter.core.service.CallAssetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CallAssetServiceImplTest {

    @Autowired
    private CallAssetService callAssetService;

    @Test
    void save_recording_and_summary_updates_same_call_id() {
        String callId = "asset-service-call";

        callAssetService.saveRecording(CallRecording.builder()
                .callId(callId)
                .recordingPath("/tmp/first.wav")
                .format("wav")
                .durationSec(12)
                .status("pending")
                .sourceType("stub")
                .build());

        CallRecording updatedRecording = callAssetService.saveRecording(CallRecording.builder()
                .callId(callId)
                .recordingPath("/tmp/final.wav")
                .format("wav")
                .durationSec(18)
                .status("success")
                .sourceType("real")
                .build());

        callAssetService.saveSummary(CallSummary.builder()
                .callId(callId)
                .summaryText("首次摘要")
                .summaryStatus("pending")
                .resultCode("processing")
                .sourceType("stub")
                .build());

        CallSummary updatedSummary = callAssetService.saveSummary(CallSummary.builder()
                .callId(callId)
                .summaryText("最终摘要")
                .summaryStatus("success")
                .resultCode("done")
                .tags("resolved")
                .sourceType("real")
                .build());

        assertThat(updatedRecording.getId()).isNotNull();
        assertThat(callAssetService.getRecording(callId)).get()
                .extracting(CallRecording::getRecordingPath, CallRecording::getDurationSec, CallRecording::getSourceType)
                .containsExactly("/tmp/final.wav", 18, "real");

        assertThat(updatedSummary.getId()).isNotNull();
        assertThat(callAssetService.getSummary(callId)).get()
                .extracting(CallSummary::getSummaryText, CallSummary::getSummaryStatus, CallSummary::getSourceType)
                .containsExactly("最终摘要", "success", "real");
    }

    @Test
    void save_transfer_result_preserves_multiple_attempts() {
        String callId = "transfer-attempt-call";

        callAssetService.saveTransferResult(TransferResult.builder()
                .callId(callId)
                .attemptNo(1)
                .transferStatus("failed")
                .failureReason("fs_error")
                .fallbackAction("retry")
                .build());

        callAssetService.saveTransferResult(TransferResult.builder()
                .callId(callId)
                .attemptNo(2)
                .targetAgentId(21L)
                .targetExtensionNo("8102")
                .transferStatus("success")
                .build());

        assertThat(callAssetService.listTransferResults(callId))
                .extracting(TransferResult::getAttemptNo, TransferResult::getTransferStatus)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1, "failed"),
                        org.assertj.core.groups.Tuple.tuple(2, "success")
                );
    }
}
