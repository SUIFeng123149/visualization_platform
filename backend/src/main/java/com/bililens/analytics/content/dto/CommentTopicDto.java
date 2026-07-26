package com.bililens.analytics.content.dto;

public record CommentTopicDto(
        String topic,
        long interactionCount,
        long negativeCount
) {
}
