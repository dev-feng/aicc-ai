package com.callcenter.core.controller;

import com.callcenter.common.exception.BusinessException;
import com.callcenter.common.exception.ErrorCode;
import com.callcenter.common.result.Result;
import com.callcenter.core.dto.CallLogQueryRequest;
import com.callcenter.core.dto.CallLogResponse;
import com.callcenter.core.dto.OutboundCallRequest;
import com.callcenter.core.dto.OutboundCallResponse;
import com.callcenter.core.service.CallLogService;
import com.callcenter.core.service.CallService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 呼叫相关接口。
 */
@RestController
@RequestMapping("/api/call")
public class CallController {

    private final CallService callService;
    private final CallLogService callLogService;

    public CallController(CallService callService, CallLogService callLogService) {
        this.callService = callService;
        this.callLogService = callLogService;
    }

    @PostMapping("/outbound")
    public Result<OutboundCallResponse> outbound(@RequestBody OutboundCallRequest request) {
        if (request == null
                || !StringUtils.hasText(request.getCaller())
                || !StringUtils.hasText(request.getCallee())) {
            return Result.fail(ErrorCode.BAD_REQUEST.getCode(), "参数错误");
        }
        try {
            String callId = callService.outbound(request.getCaller(), request.getCallee());
            return Result.success(new OutboundCallResponse(callId));
        } catch (BusinessException ex) {
            return Result.fail(ex.getCode(), ex.getMessage());
        }
    }

    @GetMapping("/log")
    public Result<List<CallLogResponse>> queryLogs(CallLogQueryRequest request) {
        if (request == null || !StringUtils.hasText(request.getPhone())) {
            return Result.fail(ErrorCode.BAD_REQUEST.getCode(), "参数错误");
        }
        return Result.success(callLogService.queryLogs(
                request.getPhone(),
                request.getStartTime(),
                request.getEndTime()
        ));
    }
}
