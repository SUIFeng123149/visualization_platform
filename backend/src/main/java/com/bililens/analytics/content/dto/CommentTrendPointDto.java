package com.bililens.analytics.content.dto;

import java.time.LocalDate;

public record CommentTrendPointDto(
        LocalDate statDate,
        long interactionCount,
        long analyzedCount,
        Double averageSentiment,
        double negativeRatio
) {
}
