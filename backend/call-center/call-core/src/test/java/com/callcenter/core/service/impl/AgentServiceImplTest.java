package com.callcenter.core.service.impl;

import com.callcenter.common.exception.BusinessException;
import com.callcenter.core.dto.AgentBindExtensionRequest;
import com.callcenter.core.dto.AgentCreateRequest;
import com.callcenter.core.dto.AgentResponse;
import com.callcenter.core.entity.Agent;
import com.callcenter.core.entity.AgentExtensionBinding;
import com.callcenter.core.mapper.AgentExtensionBindingMapper;
import com.callcenter.core.mapper.AgentMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentServiceImplTest {

    @Test
    void create_agent_returns_created_agent() {
        AgentMapper agentMapper = mock(AgentMapper.class);
        AgentExtensionBindingMapper bindingMapper = mock(AgentExtensionBindingMapper.class);
        when(agentMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            Agent agent = invocation.getArgument(0);
            agent.setId(1L);
            return 1;
        }).when(agentMapper).insert(any(Agent.class));

        AgentServiceImpl service = new AgentServiceImpl(agentMapper, bindingMapper);
        AgentCreateRequest request = new AgentCreateRequest();
        request.setAgentCode("agent-1001");
        request.setAgentName("Alice");

        AgentResponse response = service.createAgent(request);

        assertThat(response.agentId()).isEqualTo(1L);
        assertThat(response.agentCode()).isEqualTo("agent-1001");
        assertThat(response.status()).isEqualTo("offline");
        assertThat(response.extensionNos()).isEmpty();
    }

    @Test
    void bind_extension_adds_binding_for_agent() {
        AgentMapper agentMapper = mock(AgentMapper.class);
        AgentExtensionBindingMapper bindingMapper = mock(AgentExtensionBindingMapper.class);
        when(agentMapper.selectById(1L)).thenReturn(Agent.builder()
                .id(1L)
                .agentCode("agent-1001")
                .agentName("Alice")
                .status("idle")
                .enabled(1)
                .build());
        when(bindingMapper.selectOne(any())).thenReturn(null);
        when(bindingMapper.selectList(any())).thenReturn(List.of(
                AgentExtensionBinding.builder()
                        .agentId(1L)
                        .extensionNo("1001")
                        .bindingStatus("active")
                        .build()
        ));

        AgentServiceImpl service = new AgentServiceImpl(agentMapper, bindingMapper);
        AgentBindExtensionRequest request = new AgentBindExtensionRequest();
        request.setAgentId(1L);
        request.setExtensionNo("1001");

        AgentResponse response = service.bindExtension(request);

        verify(bindingMapper, times(1)).insert(any(AgentExtensionBinding.class));
        assertThat(response.extensionNos()).containsExactly("1001");
    }

    @Test
    void unbind_extension_marks_binding_inactive() {
        AgentMapper agentMapper = mock(AgentMapper.class);
        AgentExtensionBindingMapper bindingMapper = mock(AgentExtensionBindingMapper.class);
        when(agentMapper.selectById(1L)).thenReturn(Agent.builder()
                .id(1L)
                .agentCode("agent-1001")
                .agentName("Alice")
                .status("idle")
                .enabled(1)
                .build());
        when(bindingMapper.selectOne(any())).thenReturn(AgentExtensionBinding.builder()
                .id(11L)
                .agentId(1L)
                .extensionNo("1001")
                .bindingStatus("active")
                .build());
        when(bindingMapper.selectList(any())).thenReturn(List.of());

        AgentServiceImpl service = new AgentServiceImpl(agentMapper, bindingMapper);
        AgentBindExtensionRequest request = new AgentBindExtensionRequest();
        request.setAgentId(1L);
        request.setExtensionNo("1001");

        AgentResponse response = service.unbindExtension(request);

        verify(bindingMapper).updateById(any(AgentExtensionBinding.class));
        assertThat(response.extensionNos()).isEmpty();
    }

    @Test
    void get_agent_throws_when_agent_missing() {
        AgentMapper agentMapper = mock(AgentMapper.class);
        AgentExtensionBindingMapper bindingMapper = mock(AgentExtensionBindingMapper.class);
        when(agentMapper.selectById(99L)).thenReturn(null);

        AgentServiceImpl service = new AgentServiceImpl(agentMapper, bindingMapper);

        assertThatThrownBy(() -> service.getAgent(99L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("agent不存在");
    }
}
