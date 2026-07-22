package com.bililens.analytics.content.dto;

import java.time.LocalDateTime;

public record ContentSummaryDto(
        long contentId,
        String platformCode,
        String externalContentId,
        String contentType,
        Long parentContentId,
        String title,
        String accountName,
        String category,
        LocalDateTime publishedAt,
        Long viewCount,
        Long likeCount,
        Long commentCount,
        Long shareCount,
        Long favoriteCount,
        Long danmakuCount,
        Long coinCount,
        Double platformHeatScore,
        Double normalizedHeatScore,
        LocalDateTime metricsCapturedAt
) {
}
