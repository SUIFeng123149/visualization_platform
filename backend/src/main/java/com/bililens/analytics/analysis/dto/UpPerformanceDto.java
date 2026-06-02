package com.bililens.analytics.analysis.dto;

public record UpPerformanceDto(
        String upName,
        long videoCount,
        double avgViewCount,
        double avgHeatScore,
        double avgSentiment,
        long totalLikeCount
) {
}
