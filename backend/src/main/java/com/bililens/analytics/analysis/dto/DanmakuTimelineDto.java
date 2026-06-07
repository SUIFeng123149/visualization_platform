package com.bililens.analytics.analysis.dto;

public record DanmakuTimelineDto(
        String bvid,
        String title,
        int timeBucket,
        long danmakuCount,
        double avgSentiment,
        String topWords
) {
}
