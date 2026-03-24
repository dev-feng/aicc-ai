package com.callcenter.core.service.impl;

import com.callcenter.common.exception.BusinessException;
import com.callcenter.core.entity.Agent;
import com.callcenter.core.entity.AgentExtensionBinding;
import com.callcenter.core.event.CallCreatedEvent;
import com.callcenter.core.mapper.AgentExtensionBindingMapper;
import com.callcenter.core.mapper.AgentMapper;
import com.callcenter.core.model.CallSession;
import com.callcenter.core.service.AgentRoutingService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentRoutingServiceImplTest {

    @Test
    void transfer_to_specific_agent_returns_bound_extension() {
        AgentMapper agentMapper = mock(AgentMapper.class);
        AgentExtensionBindingMapper bindingMapper = mock(AgentExtensionBindingMapper.class);
        CallSessionServiceImpl sessionService = new CallSessionServiceImpl();
        sessionService.rememberCallCreated(new CallCreatedEvent("call-1", "1001", "1002", LocalDateTime.now(), 1));
        when(agentMapper.selectById(1L)).thenReturn(Agent.builder().id(1L).enabled(1).build());
        when(bindingMapper.selectOne(any())).thenReturn(AgentExtensionBinding.builder()
                .agentId(1L)
                .extensionNo("1001")
                .bindingStatus("active")
                .build());

        AgentRoutingService service = new AgentRoutingServiceImpl(agentMapper, bindingMapper, sessionService);

        AgentRoutingService.AgentRoutingResult result = service.transferToAgent("call-1", 1L);

        assertThat(result.agentId()).isEqualTo(1L);
        assertThat(result.extensionNo()).isEqualTo("1001");
        assertThat(result.status()).isEqualTo(CallSession.STATUS_TRANSFERRED);
        assertThat(sessionService.getSession("call-1").orElseThrow().getCurrentStatus())
                .isEqualTo(CallSession.STATUS_TRANSFERRED);
    }

    @Test
    void transfer_without_available_agent_marks_transfer_failed() {
        AgentMapper agentMapper = mock(AgentMapper.class);
        AgentExtensionBindingMapper bindingMapper = mock(AgentExtensionBindingMapper.class);
        CallSessionServiceImpl sessionService = new CallSessionServiceImpl();
        sessionService.rememberCallCreated(new CallCreatedEvent("call-2", "1001", "1002", LocalDateTime.now(), 1));
        when(bindingMapper.selectOne(any())).thenReturn(null);

        AgentRoutingService service = new AgentRoutingServiceImpl(agentMapper, bindingMapper, sessionService);

        assertThatThrownBy(() -> service.transferToAgent("call-2", null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("当前没有可用坐席");
        assertThat(sessionService.getSession("call-2").orElseThrow().getCurrentStatus())
                .isEqualTo(CallSession.STATUS_TRANSFER_FAILED);
    }
}
