package com.callcenter.core.mapper;

import com.callcenter.core.entity.Agent;
import com.callcenter.core.entity.AgentExtensionBinding;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 第二阶段坐席和分机绑定基础数据模型验证。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AgentMapperTest {

    @Autowired
    private AgentMapper agentMapper;

    @Autowired
    private AgentExtensionBindingMapper agentExtensionBindingMapper;

    @Test
    void insert_agent_and_binding_successfully() {
        Agent agent = Agent.builder()
                .agentCode("agent-1001")
                .agentName("Alice")
                .status("idle")
                .enabled(1)
                .build();

        int agentRows = agentMapper.insert(agent);

        assertThat(agentRows).isEqualTo(1);
        assertThat(agent.getId()).isNotNull();

        AgentExtensionBinding binding = AgentExtensionBinding.builder()
                .agentId(agent.getId())
                .extensionNo("1001")
                .bindingStatus("active")
                .build();

        int bindingRows = agentExtensionBindingMapper.insert(binding);

        assertThat(bindingRows).isEqualTo(1);
        assertThat(binding.getId()).isNotNull();

        Agent foundAgent = agentMapper.selectById(agent.getId());
        AgentExtensionBinding foundBinding = agentExtensionBindingMapper.selectById(binding.getId());

        assertThat(foundAgent).isNotNull();
        assertThat(foundAgent.getAgentCode()).isEqualTo("agent-1001");
        assertThat(foundAgent.getStatus()).isEqualTo("idle");

        assertThat(foundBinding).isNotNull();
        assertThat(foundBinding.getAgentId()).isEqualTo(agent.getId());
        assertThat(foundBinding.getExtensionNo()).isEqualTo("1001");
        assertThat(foundBinding.getBindingStatus()).isEqualTo("active");
    }
}
