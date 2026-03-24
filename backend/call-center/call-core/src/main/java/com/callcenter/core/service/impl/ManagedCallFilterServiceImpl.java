package com.callcenter.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.callcenter.core.entity.AgentExtensionBinding;
import com.callcenter.core.mapper.AgentExtensionBindingMapper;
import com.callcenter.core.service.ManagedCallFilterService;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class ManagedCallFilterServiceImpl implements ManagedCallFilterService {

    private static final String ACTIVE = "active";
    private static final String VOICEMAIL = "voicemail";
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?\\d{2,20}$");

    private final AgentExtensionBindingMapper agentExtensionBindingMapper;

    public ManagedCallFilterServiceImpl(AgentExtensionBindingMapper agentExtensionBindingMapper) {
        this.agentExtensionBindingMapper = agentExtensionBindingMapper;
    }

    @Override
    public ManagedCallDecision evaluate(String callId, String caller, String callee) {
        if (isVoicemail(caller) || isVoicemail(callee)) {
            return ManagedCallDecision.rejected("voicemail branch");
        }
        if (isBlank(caller) && isBlank(callee)) {
            return ManagedCallDecision.rejected("missing caller and callee");
        }
        if (isAbnormalNumber(caller)) {
            return ManagedCallDecision.rejected("abnormal caller: " + caller);
        }
        if (isAbnormalNumber(callee)) {
            return ManagedCallDecision.rejected("abnormal callee: " + callee);
        }

        AgentExtensionBinding matchedBinding = findActiveBinding(callee);
        if (matchedBinding != null) {
            return ManagedCallDecision.accepted(matchedBinding.getAgentId(), matchedBinding.getExtensionNo());
        }

        matchedBinding = findActiveBinding(caller);
        if (matchedBinding != null) {
            return ManagedCallDecision.accepted(matchedBinding.getAgentId(), matchedBinding.getExtensionNo());
        }

        return ManagedCallDecision.rejected("unmanaged call: " + (isBlank(callId) ? "unknown" : callId));
    }

    private AgentExtensionBinding findActiveBinding(String extensionNo) {
        if (isBlank(extensionNo)) {
            return null;
        }
        return agentExtensionBindingMapper.selectOne(new LambdaQueryWrapper<AgentExtensionBinding>()
                .eq(AgentExtensionBinding::getExtensionNo, extensionNo)
                .eq(AgentExtensionBinding::getBindingStatus, ACTIVE)
                .last("limit 1"));
    }

    private boolean isVoicemail(String value) {
        return !isBlank(value) && VOICEMAIL.equalsIgnoreCase(value.trim());
    }

    private boolean isAbnormalNumber(String value) {
        if (isBlank(value)) {
            return false;
        }
        return !PHONE_PATTERN.matcher(value.trim()).matches();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
