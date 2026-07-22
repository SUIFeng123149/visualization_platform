package com.bililens.analytics.content.dto;

import java.time.LocalDateTime;

public record InteractionDto(
        long interactionId,
        long contentId,
        String platformCode,
        String externalInteractionId,
        String interactionType,
        String userName,
        String text,
        Long likeCount,
        Double videoTimeSeconds,
        LocalDateTime occurredAt,
        LocalDateTime capturedAt,
        Double sentimentScore,
        String sentimentLabel
) {
}
