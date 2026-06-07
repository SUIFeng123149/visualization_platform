package com.bililens.analytics.platform.dto;

import java.time.LocalDateTime;

public record AnomalyRuleDto(
        Long id,
        String ruleKey,
        String name,
        String metric,
        String operator,
        double threshold,
        String level,
        boolean enabled,
        String description,
        LocalDateTime updatedAt
) {
}
