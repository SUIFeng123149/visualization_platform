package com.bililens.analytics.analysis.dto;

public record VideoHeatRankDto(
        String bvid,
        String title,
        String upName,
        String category,
        long viewCount,
        long likeCount,
        long coinCount,
        long favoriteCount,
        long replyCount,
        long danmakuCount,
        double heatScore,
        int rankNo
) {
}
