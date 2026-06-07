package com.bililens.analytics.analysis.dto;

import java.util.List;

public record VideoDetailDto(
        VideoHeatRankDto video,
        VideoSentimentDto sentiment,
        List<DanmakuTimelineDto> danmakuTimeline,
        List<KeywordTopDto> keywords,
        List<NegativeCommentDto> negativeComments,
        List<ActionInsightDto> insights
) {
}
