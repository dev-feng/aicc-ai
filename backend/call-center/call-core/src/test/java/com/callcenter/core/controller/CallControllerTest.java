package com.callcenter.core.controller;

import com.callcenter.common.exception.BusinessException;
import com.callcenter.common.exception.ErrorCode;
import com.callcenter.core.service.CallService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CallController.class)
class CallControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CallService callService;

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
                .andExpect(jsonPath("$.msg").value("参数错误"));
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
}
