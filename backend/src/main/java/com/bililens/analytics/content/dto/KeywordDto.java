package com.bililens.analytics.content.dto;

public record KeywordDto(
        String word,
        long wordCount,
        int rankNo
) {
}
