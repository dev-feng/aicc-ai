package com.callcenter.core.controller;

import com.callcenter.common.result.Result;
import com.callcenter.core.dto.AgentBindExtensionRequest;
import com.callcenter.core.dto.AgentCreateRequest;
import com.callcenter.core.dto.AgentResponse;
import com.callcenter.core.service.AgentService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 坐席管理接口。
 */
@Validated
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping
    public Result<AgentResponse> createAgent(@Valid @RequestBody AgentCreateRequest request) {
        return Result.success(agentService.createAgent(request));
    }

    @GetMapping("/{agentId}")
    public Result<AgentResponse> getAgent(@PathVariable Long agentId) {
        return Result.success(agentService.getAgent(agentId));
    }

    @PostMapping("/bind-extension")
    public Result<AgentResponse> bindExtension(@Valid @RequestBody AgentBindExtensionRequest request) {
        return Result.success(agentService.bindExtension(request));
    }

    @PostMapping("/unbind-extension")
    public Result<AgentResponse> unbindExtension(@Valid @RequestBody AgentBindExtensionRequest request) {
        return Result.success(agentService.unbindExtension(request));
    }
}
