package com.bililens.analytics.ai.dto;

import jakarta.validation.constraints.Size;

import java.util.Map;

public record AiChatRequest(
        @Size(max = 4000) String query,
        @Size(max = 128) String conversationId,
        @Size(max = 128) String user,
        @Size(max = 64) String mode,
        Map<String, Object> context
) {
}
