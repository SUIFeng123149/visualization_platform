package com.bililens.analytics.analysis.dto;

import java.time.LocalDateTime;

public record NegativeCommentDto(
        String bvid,
        String rpid,
        String userName,
        String cleanContent,
        long likeCount,
        LocalDateTime crawledAt,
        double sentimentScore
) {
}
