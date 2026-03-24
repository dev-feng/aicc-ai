package com.callcenter.core.service.impl;

import com.callcenter.core.entity.AgentExtensionBinding;
import com.callcenter.core.mapper.AgentExtensionBindingMapper;
import com.callcenter.core.service.ManagedCallFilterService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ManagedCallFilterServiceImplTest {

    @Test
    void evaluate_rejects_voicemail_branch() {
        AgentExtensionBindingMapper mapper = mock(AgentExtensionBindingMapper.class);
        ManagedCallFilterService service = new ManagedCallFilterServiceImpl(mapper);

        ManagedCallFilterService.ManagedCallDecision decision = service.evaluate("call-1", "1002", "voicemail");

        assertThat(decision.accepted()).isFalse();
        assertThat(decision.reason()).contains("voicemail");
    }

    @Test
    void evaluate_rejects_abnormal_number() {
        AgentExtensionBindingMapper mapper = mock(AgentExtensionBindingMapper.class);
        ManagedCallFilterService service = new ManagedCallFilterServiceImpl(mapper);

        ManagedCallFilterService.ManagedCallDecision decision = service.evaluate("call-1", "sip:scanner", "1002");

        assertThat(decision.accepted()).isFalse();
        assertThat(decision.reason()).contains("abnormal caller");
    }

    @Test
    void evaluate_accepts_call_when_callee_is_managed_extension() {
        AgentExtensionBindingMapper mapper = mock(AgentExtensionBindingMapper.class);
        when(mapper.selectOne(any())).thenReturn(AgentExtensionBinding.builder()
                .agentId(7L)
                .extensionNo("1002")
                .bindingStatus("active")
                .build());
        ManagedCallFilterService service = new ManagedCallFilterServiceImpl(mapper);

        ManagedCallFilterService.ManagedCallDecision decision = service.evaluate("call-1", "13800000000", "1002");

        assertThat(decision.accepted()).isTrue();
        assertThat(decision.agentId()).isEqualTo(7L);
        assertThat(decision.extensionNo()).isEqualTo("1002");
    }
}
