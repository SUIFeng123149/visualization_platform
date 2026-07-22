package com.bililens.analytics.content.dto;

public record SentimentSummaryDto(
        long contentId,
        long totalCount,
        long positiveCount,
        long neutralCount,
        long negativeCount,
        Double averageScore,
        double positiveRatio,
        double negativeRatio
) {
}
