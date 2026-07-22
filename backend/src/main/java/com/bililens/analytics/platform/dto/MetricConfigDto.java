package com.bililens.analytics.platform.dto;

public record MetricConfigDto(
        String metricKey, String displayName, String unit, String scope,
        String definition, boolean comparable
) {
}
