package com.bililens.analytics.task.dto;

import java.time.LocalDateTime;

public record TaskDto(
        String taskId,
        String title,
        String level,
        String type,
        String text,
        Long contentId,
        String platformCode,
        String externalContentId,
        String source,
        int sortNo,
        String status,
        LocalDateTime statusUpdatedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
