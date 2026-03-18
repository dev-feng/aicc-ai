package com.callcenter.core.controller;

import com.callcenter.common.result.Result;
import com.callcenter.core.dto.CallLogQueryRequest;
import com.callcenter.core.dto.CallLogResponse;
import com.callcenter.core.dto.OutboundCallRequest;
import com.callcenter.core.dto.OutboundCallResponse;
import com.callcenter.core.service.CallLogService;
import com.callcenter.core.service.CallService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 呼叫相关接口。
 */
@Validated
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
    public Result<OutboundCallResponse> outbound(@Valid @RequestBody OutboundCallRequest request) {
        String callId = callService.outbound(request.getCaller(), request.getCallee());
        return Result.success(new OutboundCallResponse(callId));
    }

    @GetMapping("/log")
    public Result<List<CallLogResponse>> queryLogs(@Valid @ModelAttribute CallLogQueryRequest request) {
        return Result.success(callLogService.queryLogs(
                request.getPhone(),
                request.getStartTime(),
                request.getEndTime()
        ));
    }
}
