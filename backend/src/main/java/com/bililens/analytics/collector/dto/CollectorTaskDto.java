package com.bililens.analytics.collector.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CollectorTaskDto(
        String taskId,
        String externalTaskId,
        String taskName,
        String naturalLanguage,
        String sourceType,
        String collectMode,
        String status,
        Integer progress,
        String priority,
        String message,
        Long rowCount,
        String rawHdfsPath,
        String cleanHdfsPath,
        String batchId,
        List<String> resultTables,
        String paramsJson,
        String callbackUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime dispatchedAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {
}
