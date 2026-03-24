package com.callcenter.core.ai;

import com.callcenter.core.ai.model.TtsResult;

public interface TtsService {

    TtsResult synthesize(String callId, String text);
}
