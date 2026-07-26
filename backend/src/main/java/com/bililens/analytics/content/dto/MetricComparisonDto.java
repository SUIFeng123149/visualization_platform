package com.bililens.analytics.content.dto;

import java.time.LocalDateTime;

public record MetricComparisonDto(
        String metricKey,
        String platformCode,
        long contentCount,
        long availableCount,
        Double averageValue,
        Double minValue,
        Double maxValue,
        LocalDateTime latestCapturedAt
) {
}
