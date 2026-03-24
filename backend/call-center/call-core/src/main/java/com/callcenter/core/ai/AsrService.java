package com.callcenter.core.ai;

import com.callcenter.core.ai.model.AsrResult;

public interface AsrService {

    AsrResult transcribe(String callId, byte[] audioPayload);
}
