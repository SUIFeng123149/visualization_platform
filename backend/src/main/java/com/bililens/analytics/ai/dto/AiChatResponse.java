package com.bililens.analytics.ai.dto;

public record AiChatResponse(
        String answer,
        String conversationId,
        boolean configured
) {
}
