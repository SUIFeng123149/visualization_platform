package com.bililens.analytics.content.dto;

public record MetricComparisonDto(
        String metricKey,
        String platformCode,
        long contentCount,
        long availableCount,
        Double averageValue,
        Double minValue,
        Double maxValue
) {
}
