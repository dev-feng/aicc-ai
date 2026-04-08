package com.callcenter.core.service;

import com.callcenter.core.entity.CallRecording;
import com.callcenter.core.entity.CallSummary;
import com.callcenter.core.entity.TransferResult;

import java.util.List;
import java.util.Optional;

/**
 * 第三阶段最小沉淀数据持久化服务。
 */
public interface CallAssetService {

    CallRecording saveRecording(CallRecording recording);

    CallSummary saveSummary(CallSummary summary);

    TransferResult saveTransferResult(TransferResult transferResult);

    Optional<CallRecording> getRecording(String callId);

    Optional<CallSummary> getSummary(String callId);

    List<TransferResult> listTransferResults(String callId);
}
