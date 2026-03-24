package com.callcenter.core.ai.impl;

import com.callcenter.core.ai.LlmService;
import com.callcenter.core.ai.model.LlmResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "call.core", name = "mock-ai-enabled", havingValue = "true", matchIfMissing = true)
public class MockLlmServiceImpl implements LlmService {

    @Override
    public LlmResult generateReply(String callId, String transcript) {
        String normalizedTranscript = transcript == null || transcript.isBlank() ? "未识别到有效语音输入" : transcript;
        String intentCode = normalizedTranscript.contains("人工") ? "transfer_to_agent" : "collect_info";
        boolean transferToHuman = "transfer_to_agent".equals(intentCode);
        String replyText = transferToHuman
                ? "好的，正在为您转接人工坐席。"
                : "收到，系统已记录您的诉求，我们继续下一步。";
        return new LlmResult(replyText, intentCode, transferToHuman, true);
    }
}
