package com.callcenter.core.controller;

import com.callcenter.common.exception.BusinessException;
import com.callcenter.common.exception.ErrorCode;
import com.callcenter.core.dto.AgentResponse;
import com.callcenter.core.service.AgentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AgentController.class)
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AgentService agentService;

    @Test
    void create_agent_returns_success_result() throws Exception {
        when(agentService.createAgent(any())).thenReturn(new AgentResponse(
                1L, "agent-1001", "Alice", "offline", 1, List.of()
        ));

        mockMvc.perform(post("/api/agent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"agentCode":"agent-1001","agentName":"Alice"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.agentId").value(1))
                .andExpect(jsonPath("$.data.agentCode").value("agent-1001"));
    }

    @Test
    void create_agent_returns_bad_request_when_parameter_missing() throws Exception {
        mockMvc.perform(post("/api/agent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"agentCode":"","agentName":"Alice"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("agentCode: agentCode不能为空"));
    }

    @Test
    void get_agent_returns_result() throws Exception {
        when(agentService.getAgent(eq(1L))).thenReturn(new AgentResponse(
                1L, "agent-1001", "Alice", "idle", 1, List.of("1001")
        ));

        mockMvc.perform(get("/api/agent/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.extensionNos[0]").value("1001"));
    }

    @Test
    void bind_extension_returns_internal_result_when_service_fails() throws Exception {
        when(agentService.bindExtension(any()))
                .thenThrow(new BusinessException(ErrorCode.BAD_REQUEST, "extensionNo已被其他坐席绑定"));

        mockMvc.perform(post("/api/agent/bind-extension")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"agentId":1,"extensionNo":"1001"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("extensionNo已被其他坐席绑定"));
    }

    @Test
    void unbind_extension_returns_success() throws Exception {
        when(agentService.unbindExtension(any())).thenReturn(new AgentResponse(
                1L, "agent-1001", "Alice", "idle", 1, List.of()
        ));

        mockMvc.perform(post("/api/agent/unbind-extension")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"agentId":1,"extensionNo":"1001"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.extensionNos").isArray())
                .andExpect(jsonPath("$.data.extensionNos").isEmpty());
    }
}
