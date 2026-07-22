package com.bililens.analytics.content.dto;

public record MetricDefinitionDto(
        String metricKey,
        String displayName,
        String unit,
        String scope,
        String definition,
        boolean comparable
) {
}
