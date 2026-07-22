package com.bililens.analytics.content.dto;

public record CommentInsightSummaryDto(
        String platformCode,
        String interactionType,
        long interactionCount,
        long analyzedCount,
        long positiveCount,
        long neutralCount,
        long negativeCount,
        Double averageSentiment,
        double positiveRatio,
        double neutralRatio,
        double negativeRatio
) {
}
