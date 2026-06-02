package com.bililens.analytics.analysis.dto;

public record KeywordTopDto(
        String word,
        long wordCount,
        int rankNo
) {
}
