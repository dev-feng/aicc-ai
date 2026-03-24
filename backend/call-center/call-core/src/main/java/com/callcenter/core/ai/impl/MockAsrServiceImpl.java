package com.callcenter.core.ai.impl;

import com.callcenter.core.ai.AsrService;
import com.callcenter.core.ai.model.AsrResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@ConditionalOnProperty(prefix = "call.core", name = "mock-ai-enabled", havingValue = "true", matchIfMissing = true)
public class MockAsrServiceImpl implements AsrService {

    @Override
    public AsrResult transcribe(String callId, byte[] audioPayload) {
        String transcript = (audioPayload == null || audioPayload.length == 0)
                ? "mock_asr_empty_input"
                : new String(audioPayload, StandardCharsets.UTF_8);
        return new AsrResult(transcript, 0.98d, true);
    }
}
