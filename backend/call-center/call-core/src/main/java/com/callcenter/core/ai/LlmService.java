package com.callcenter.core.ai;

import com.callcenter.core.ai.model.LlmResult;

public interface LlmService {

    LlmResult generateReply(String callId, String transcript);
}
