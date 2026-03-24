package com.callcenter.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.callcenter.common.exception.BusinessException;
import com.callcenter.common.exception.ErrorCode;
import com.callcenter.core.dto.AgentBindExtensionRequest;
import com.callcenter.core.dto.AgentCreateRequest;
import com.callcenter.core.dto.AgentResponse;
import com.callcenter.core.entity.Agent;
import com.callcenter.core.entity.AgentExtensionBinding;
import com.callcenter.core.mapper.AgentExtensionBindingMapper;
import com.callcenter.core.mapper.AgentMapper;
import com.callcenter.core.service.AgentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 坐席管理服务实现。
 */
@Service
public class AgentServiceImpl implements AgentService {

    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_INACTIVE = "inactive";

    private final AgentMapper agentMapper;
    private final AgentExtensionBindingMapper bindingMapper;

    public AgentServiceImpl(AgentMapper agentMapper, AgentExtensionBindingMapper bindingMapper) {
        this.agentMapper = agentMapper;
        this.bindingMapper = bindingMapper;
    }

    @Override
    @Transactional
    public AgentResponse createAgent(AgentCreateRequest request) {
        Agent existing = agentMapper.selectOne(new LambdaQueryWrapper<Agent>()
                .eq(Agent::getAgentCode, request.getAgentCode())
                .last("limit 1"));
        if (existing != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "agentCode已存在");
        }

        Agent agent = Agent.builder()
                .agentCode(request.getAgentCode())
                .agentName(request.getAgentName())
                .status("offline")
                .enabled(1)
                .build();
        agentMapper.insert(agent);
        return toResponse(agent, List.of());
    }

    @Override
    public AgentResponse getAgent(Long agentId) {
        Agent agent = requireAgent(agentId);
        return toResponse(agent, findActiveExtensions(agentId));
    }

    @Override
    @Transactional
    public AgentResponse bindExtension(AgentBindExtensionRequest request) {
        Agent agent = requireAgent(request.getAgentId());
        AgentExtensionBinding binding = bindingMapper.selectOne(new LambdaQueryWrapper<AgentExtensionBinding>()
                .eq(AgentExtensionBinding::getExtensionNo, request.getExtensionNo())
                .last("limit 1"));

        if (binding == null) {
            binding = AgentExtensionBinding.builder()
                    .agentId(agent.getId())
                    .extensionNo(request.getExtensionNo())
                    .bindingStatus(STATUS_ACTIVE)
                    .build();
            bindingMapper.insert(binding);
            return toResponse(agent, findActiveExtensions(agent.getId()));
        }

        if (STATUS_ACTIVE.equals(binding.getBindingStatus()) && !binding.getAgentId().equals(agent.getId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "extensionNo已被其他坐席绑定");
        }

        if (STATUS_ACTIVE.equals(binding.getBindingStatus())) {
            return toResponse(agent, findActiveExtensions(agent.getId()));
        }

        binding.setAgentId(agent.getId());
        binding.setBindingStatus(STATUS_ACTIVE);
        bindingMapper.updateById(binding);
        return toResponse(agent, findActiveExtensions(agent.getId()));
    }

    @Override
    @Transactional
    public AgentResponse unbindExtension(AgentBindExtensionRequest request) {
        Agent agent = requireAgent(request.getAgentId());
        AgentExtensionBinding binding = bindingMapper.selectOne(new LambdaQueryWrapper<AgentExtensionBinding>()
                .eq(AgentExtensionBinding::getAgentId, request.getAgentId())
                .eq(AgentExtensionBinding::getExtensionNo, request.getExtensionNo())
                .eq(AgentExtensionBinding::getBindingStatus, STATUS_ACTIVE)
                .last("limit 1"));

        if (binding == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "extensionNo未绑定到当前坐席");
        }

        binding.setBindingStatus(STATUS_INACTIVE);
        bindingMapper.updateById(binding);
        return toResponse(agent, findActiveExtensions(agent.getId()));
    }

    private Agent requireAgent(Long agentId) {
        Agent agent = agentMapper.selectById(agentId);
        if (agent == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "agent不存在");
        }
        return agent;
    }

    private List<String> findActiveExtensions(Long agentId) {
        return bindingMapper.selectList(new LambdaQueryWrapper<AgentExtensionBinding>()
                        .eq(AgentExtensionBinding::getAgentId, agentId)
                        .eq(AgentExtensionBinding::getBindingStatus, STATUS_ACTIVE))
                .stream()
                .map(AgentExtensionBinding::getExtensionNo)
                .toList();
    }

    private AgentResponse toResponse(Agent agent, List<String> extensionNos) {
        return new AgentResponse(
                agent.getId(),
                agent.getAgentCode(),
                agent.getAgentName(),
                agent.getStatus(),
                agent.getEnabled(),
                extensionNos
        );
    }
}
