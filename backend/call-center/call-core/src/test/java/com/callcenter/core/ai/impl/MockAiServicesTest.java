package com.callcenter.core.ai.impl;

import com.callcenter.core.ai.model.AsrResult;
import com.callcenter.core.ai.model.LlmResult;
import com.callcenter.core.ai.model.TtsResult;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class MockAiServicesTest {

    @Test
    void mock_asr_transcribes_plain_text_payload() {
        MockAsrServiceImpl service = new MockAsrServiceImpl();

        AsrResult result = service.transcribe("call-1", "客户需要人工服务".getBytes(StandardCharsets.UTF_8));

        assertThat(result.transcript()).isEqualTo("客户需要人工服务");
        assertThat(result.confidence()).isEqualTo(0.98d);
        assertThat(result.mock()).isTrue();
    }

    @Test
    void mock_llm_can_mark_transfer_intent() {
        MockLlmServiceImpl service = new MockLlmServiceImpl();

        LlmResult result = service.generateReply("call-1", "我要人工");

        assertThat(result.intentCode()).isEqualTo("transfer_to_agent");
        assertThat(result.transferToHuman()).isTrue();
        assertThat(result.mock()).isTrue();
    }

    @Test
    void mock_tts_returns_audio_payload() {
        MockTtsServiceImpl service = new MockTtsServiceImpl();

        TtsResult result = service.synthesize("call-1", "您好，这里是测试语音");

        assertThat(result.text()).isEqualTo("您好，这里是测试语音");
        assertThat(result.audioPayload()).isNotEmpty();
        assertThat(result.audioFormat()).isEqualTo("mock/pcm");
        assertThat(result.mock()).isTrue();
    }
}
