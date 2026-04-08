package com.callcenter.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.callcenter.common.exception.BusinessException;
import com.callcenter.common.exception.ErrorCode;
import com.callcenter.core.entity.CallRecording;
import com.callcenter.core.entity.CallSummary;
import com.callcenter.core.entity.TransferResult;
import com.callcenter.core.mapper.CallRecordingMapper;
import com.callcenter.core.mapper.CallSummaryMapper;
import com.callcenter.core.mapper.TransferResultMapper;
import com.callcenter.core.service.CallAssetService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 第三阶段最小录音/摘要/转接结果持久化实现。
 */
@Service
public class CallAssetServiceImpl implements CallAssetService {

    private final CallRecordingMapper callRecordingMapper;
    private final CallSummaryMapper callSummaryMapper;
    private final TransferResultMapper transferResultMapper;

    public CallAssetServiceImpl(
            CallRecordingMapper callRecordingMapper,
            CallSummaryMapper callSummaryMapper,
            TransferResultMapper transferResultMapper
    ) {
        this.callRecordingMapper = callRecordingMapper;
        this.callSummaryMapper = callSummaryMapper;
        this.transferResultMapper = transferResultMapper;
    }

    @Override
    @Transactional
    public CallRecording saveRecording(CallRecording recording) {
        validateCallId(recording == null ? null : recording.getCallId(), "recording.callId不能为空");
        CallRecording existing = callRecordingMapper.selectOne(new LambdaQueryWrapper<CallRecording>()
                .eq(CallRecording::getCallId, recording.getCallId())
                .last("limit 1"));
        if (existing == null) {
            callRecordingMapper.insert(recording);
            return recording;
        }
        recording.setId(existing.getId());
        callRecordingMapper.updateById(recording);
        return callRecordingMapper.selectById(existing.getId());
    }

    @Override
    @Transactional
    public CallSummary saveSummary(CallSummary summary) {
        validateCallId(summary == null ? null : summary.getCallId(), "summary.callId不能为空");
        CallSummary existing = callSummaryMapper.selectOne(new LambdaQueryWrapper<CallSummary>()
                .eq(CallSummary::getCallId, summary.getCallId())
                .last("limit 1"));
        if (existing == null) {
            callSummaryMapper.insert(summary);
            return summary;
        }
        summary.setId(existing.getId());
        callSummaryMapper.updateById(summary);
        return callSummaryMapper.selectById(existing.getId());
    }

    @Override
    @Transactional
    public TransferResult saveTransferResult(TransferResult transferResult) {
        validateCallId(transferResult == null ? null : transferResult.getCallId(), "transferResult.callId不能为空");
        if (transferResult.getAttemptNo() == null || transferResult.getAttemptNo() < 1) {
            transferResult.setAttemptNo(1);
        }
        transferResultMapper.insert(transferResult);
        return transferResult;
    }

    @Override
    public Optional<CallRecording> getRecording(String callId) {
        if (isBlank(callId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(callRecordingMapper.selectOne(new LambdaQueryWrapper<CallRecording>()
                .eq(CallRecording::getCallId, callId)
                .last("limit 1")));
    }

    @Override
    public Optional<CallSummary> getSummary(String callId) {
        if (isBlank(callId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(callSummaryMapper.selectOne(new LambdaQueryWrapper<CallSummary>()
                .eq(CallSummary::getCallId, callId)
                .last("limit 1")));
    }

    @Override
    public List<TransferResult> listTransferResults(String callId) {
        if (isBlank(callId)) {
            return List.of();
        }
        return transferResultMapper.selectList(new LambdaQueryWrapper<TransferResult>()
                .eq(TransferResult::getCallId, callId)
                .orderByAsc(TransferResult::getAttemptNo, TransferResult::getId));
    }

    private void validateCallId(String callId, String message) {
        if (isBlank(callId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, message);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
