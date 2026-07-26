package com.bililens.analytics.content.dto;

import java.time.LocalDateTime;

public record ContentMetricHistoryPointDto(
        LocalDateTime capturedAt,
        Long viewCount,
        Long likeCount,
        Long commentCount,
        Long shareCount,
        Double normalizedHeatScore
) {
}
