package com.bililens.analytics.content.dto;

import java.time.LocalDate;

public record TrendPointDto(
        LocalDate statDate,
        String interactionType,
        long interactionCount,
        Double averageSentiment
) {
}
