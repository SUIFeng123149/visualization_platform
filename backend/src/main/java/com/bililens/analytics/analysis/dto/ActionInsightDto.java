package com.bililens.analytics.analysis.dto;

public record ActionInsightDto(
        String title,
        String level,
        String type,
        String reason,
        String action
) {
}
