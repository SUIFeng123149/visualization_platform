package com.bililens.analytics.analysis.dto;

public record VideoSentimentDto(
        String bvid,
        String title,
        double avgSentiment,
        long positiveCount,
        long neutralCount,
        long negativeCount,
        long totalCount,
        double positiveRatio,
        double negativeRatio
) {
}
