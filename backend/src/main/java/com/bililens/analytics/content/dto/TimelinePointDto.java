package com.bililens.analytics.content.dto;

public record TimelinePointDto(
        long contentId,
        String interactionType,
        int timeBucket,
        long interactionCount,
        Double averageSentiment
) {
}
