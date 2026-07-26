package com.bililens.analytics.content.dto;

import java.time.LocalDateTime;

public record DashboardSummaryDto(
        long contentCount,
        long platformCount,
        long totalViews,
        Double averageNormalizedHeat,
        long interactionCount,
        LocalDateTime latestCapturedAt
) {
}
