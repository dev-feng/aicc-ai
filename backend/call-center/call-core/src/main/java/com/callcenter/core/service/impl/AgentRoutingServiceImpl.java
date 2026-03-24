package com.callcenter.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.callcenter.common.exception.BusinessException;
import com.callcenter.common.exception.ErrorCode;
import com.callcenter.core.entity.Agent;
import com.callcenter.core.entity.AgentExtensionBinding;
import com.callcenter.core.mapper.AgentExtensionBindingMapper;
import com.callcenter.core.mapper.AgentMapper;
import com.callcenter.core.model.CallSession;
import com.callcenter.core.service.AgentRoutingService;
import com.callcenter.core.service.CallSessionService;
import org.springframework.stereotype.Service;

@Service
public class AgentRoutingServiceImpl implements AgentRoutingService {

    private static final String STATUS_ACTIVE = "active";

    private final AgentMapper agentMapper;
    private final AgentExtensionBindingMapper bindingMapper;
    private final CallSessionService callSessionService;

    public AgentRoutingServiceImpl(
            AgentMapper agentMapper,
            AgentExtensionBindingMapper bindingMapper,
            CallSessionService callSessionService
    ) {
        this.agentMapper = agentMapper;
        this.bindingMapper = bindingMapper;
        this.callSessionService = callSessionService;
    }

    @Override
    public AgentRoutingResult transferToAgent(String callId, Long targetAgentId) {
        if (callId == null || callId.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "callId不能为空");
        }
        callSessionService.getSession(callId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "call session不存在"));

        AgentExtensionBinding binding = targetAgentId != null
                ? findBindingByAgent(targetAgentId)
                : findAnyAvailableBinding();

        if (binding == null) {
            callSessionService.updateSessionStatus(callId, CallSession.STATUS_TRANSFER_FAILED);
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前没有可用坐席");
        }

        callSessionService.updateSessionStatus(callId, CallSession.STATUS_TRANSFERRED);
        return new AgentRoutingResult(
                callId,
                binding.getAgentId(),
                binding.getExtensionNo(),
                CallSession.STATUS_TRANSFERRED,
                true
        );
    }

    private AgentExtensionBinding findBindingByAgent(Long agentId) {
        Agent agent = agentMapper.selectById(agentId);
        if (agent == null || agent.getEnabled() == null || agent.getEnabled() != 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "目标坐席不存在或不可用");
        }
        return bindingMapper.selectOne(new LambdaQueryWrapper<AgentExtensionBinding>()
                .eq(AgentExtensionBinding::getAgentId, agentId)
                .eq(AgentExtensionBinding::getBindingStatus, STATUS_ACTIVE)
                .last("limit 1"));
    }

    private AgentExtensionBinding findAnyAvailableBinding() {
        return bindingMapper.selectOne(new LambdaQueryWrapper<AgentExtensionBinding>()
                .eq(AgentExtensionBinding::getBindingStatus, STATUS_ACTIVE)
                .orderByAsc(AgentExtensionBinding::getAgentId)
                .last("limit 1"));
    }
}
