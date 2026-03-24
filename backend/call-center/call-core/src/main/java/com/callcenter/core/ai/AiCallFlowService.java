package com.callcenter.core.ai;

import com.callcenter.core.ai.model.AiCallFlowResult;

public interface AiCallFlowService {

    AiCallFlowResult processText(String callId, String transcript);
}
