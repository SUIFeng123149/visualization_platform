package com.bililens.analytics.content.dto;

public record AccountPerformanceDto(
        long accountId,
        String platformCode,
        String externalAccountId,
        String displayName,
        String accountType,
        long contentCount,
        Long totalViewCount,
        Long totalLikeCount,
        Double averageNormalizedHeat
) {
}
