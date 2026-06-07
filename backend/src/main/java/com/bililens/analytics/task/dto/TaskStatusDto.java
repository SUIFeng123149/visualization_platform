package com.bililens.analytics.task.dto;

import java.time.LocalDateTime;

public record TaskStatusDto(
        String taskId,
        String status,
        LocalDateTime updatedAt,
        LocalDateTime createdAt
) {
}
