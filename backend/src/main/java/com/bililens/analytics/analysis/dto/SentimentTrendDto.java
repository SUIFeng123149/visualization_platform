package com.bililens.analytics.analysis.dto;

import java.time.LocalDate;

public record SentimentTrendDto(
        LocalDate statDate,
        long commentCount,
        long danmakuCount,
        double avgSentiment,
        double negativeRatio
) {
}
