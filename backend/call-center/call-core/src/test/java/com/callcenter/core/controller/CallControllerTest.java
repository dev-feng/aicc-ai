package com.callcenter.core.controller;

import com.callcenter.common.exception.BusinessException;
import com.callcenter.common.exception.ErrorCode;
import com.callcenter.core.dto.CallLogResponse;
import com.callcenter.core.model.CallSession;
import com.callcenter.core.service.AgentRoutingService;
import com.callcenter.core.service.CallLogService;
import com.callcenter.core.service.CallService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CallController.class)
class CallControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CallService callService;

    @MockBean
    private CallLogService callLogService;

    @MockBean
    private AgentRoutingService agentRoutingService;

    @Test
    void outbound_returns_success_result() throws Exception {
        when(callService.outbound(eq("1000"), eq("1001"))).thenReturn("job-123");

        mockMvc.perform(post("/api/call/outbound")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"caller":"1000","callee":"1001"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("success"))
                .andExpect(jsonPath("$.data.callId").value("job-123"));
    }

    @Test
    void outbound_returns_bad_request_when_parameter_missing() throws Exception {
        mockMvc.perform(post("/api/call/outbound")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"caller":"","callee":"1001"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("caller: caller不能为空"));
    }

    @Test
    void outbound_returns_internal_error_when_service_fails() throws Exception {
        when(callService.outbound(eq("1000"), eq("1001")))
                .thenThrow(new BusinessException(ErrorCode.INTERNAL_ERROR, "mock fs error"));

        mockMvc.perform(post("/api/call/outbound")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"caller":"1000","callee":"1001"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("mock fs error"));
    }

    @Test
    void query_logs_returns_result_list() throws Exception {
        when(callLogService.queryLogs(eq("1000"), isNull(), isNull())).thenReturn(List.of(
                new CallLogResponse(
                        "call-1",
                        "outbound",
                        "1000",
                        "1001",
                        LocalDateTime.of(2026, 3, 17, 18, 0, 0),
                        LocalDateTime.of(2026, 3, 17, 18, 0, 20),
                        20,
                        "NORMAL_CLEARING"
                )
        ));

        mockMvc.perform(get("/api/call/log").param("phone", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].callId").value("call-1"))
                .andExpect(jsonPath("$.data[0].direction").value("outbound"))
                .andExpect(jsonPath("$.data[0].durationSec").value(20));
    }

    @Test
    void query_managed_logs_returns_result_list() throws Exception {
        when(callLogService.queryLogs(eq("1000"), isNull(), isNull())).thenReturn(List.of(
                new CallLogResponse(
                        "call-2",
                        "inbound",
                        "1000",
                        "1001",
                        LocalDateTime.of(2026, 3, 30, 10, 0, 0),
                        LocalDateTime.of(2026, 3, 30, 10, 0, 40),
                        40,
                        "NORMAL_CLEARING"
                )
        ));

        mockMvc.perform(get("/api/call/managed-log").param("phone", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].callId").value("call-2"))
                .andExpect(jsonPath("$.data[0].direction").value("inbound"));
    }

    @Test
    void query_logs_returns_bad_request_when_phone_missing() throws Exception {
        mockMvc.perform(get("/api/call/log"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("phone: phone不能为空"));
    }

    @Test
    void query_logs_returns_empty_list_when_no_data() throws Exception {
        when(callLogService.queryLogs(eq("1009"), isNull(), isNull())).thenReturn(List.of());

        mockMvc.perform(get("/api/call/log").param("phone", "1009"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void transfer_returns_success_result() throws Exception {
        when(agentRoutingService.transferToAgent(eq("call-123"), eq(7L)))
                .thenReturn(new AgentRoutingService.AgentRoutingResult(
                        "call-123",
                        7L,
                        "1007",
                        CallSession.STATUS_TRANSFERRED,
                        true
                ));

        mockMvc.perform(post("/api/call/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"callId":"call-123","targetAgentId":7}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.callId").value("call-123"))
                .andExpect(jsonPath("$.data.agentId").value(7))
                .andExpect(jsonPath("$.data.extensionNo").value("1007"))
                .andExpect(jsonPath("$.data.status").value("transferred"));
    }

    @Test
    void transfer_returns_bad_request_when_call_id_missing() throws Exception {
        mockMvc.perform(post("/api/call/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"callId":"","targetAgentId":7}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("callId: callId不能为空"));
    }
}
