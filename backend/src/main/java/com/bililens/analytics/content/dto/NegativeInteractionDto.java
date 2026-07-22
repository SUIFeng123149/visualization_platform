package com.bililens.analytics.content.dto;

import java.time.LocalDateTime;

public record NegativeInteractionDto(
        long interactionId,
        long contentId,
        String platformCode,
        String externalContentId,
        String contentTitle,
        String interactionType,
        String userName,
        String text,
        Long likeCount,
        LocalDateTime occurredAt,
        LocalDateTime capturedAt,
        Double sentimentScore
) {
}
