package com.callcenter.core.ai.impl;

import com.callcenter.core.ai.TtsService;
import com.callcenter.core.ai.model.TtsResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@ConditionalOnProperty(prefix = "call.core", name = "mock-ai-enabled", havingValue = "true", matchIfMissing = true)
public class MockTtsServiceImpl implements TtsService {

    @Override
    public TtsResult synthesize(String callId, String text) {
        String normalizedText = text == null || text.isBlank() ? "mock_tts_empty_text" : text;
        return new TtsResult(
                normalizedText,
                normalizedText.getBytes(StandardCharsets.UTF_8),
                "mock/pcm",
                true
        );
    }
}
