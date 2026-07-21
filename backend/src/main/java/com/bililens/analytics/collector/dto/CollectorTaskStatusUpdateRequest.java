package com.bililens.analytics.collector.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CollectorTaskStatusUpdateRequest(
        @Size(max = 32) String status,
        @Min(0) @Max(100) Integer progress,
        @Size(max = 128) String externalTaskId,
        @Size(max = 1000) String message,
        @Size(max = 500) String rawHdfsPath,
        @Size(max = 500) String cleanHdfsPath,
        @Size(max = 128) String batchId,
        List<String> resultTables,
        @Min(0) Long rowCount
) {
}
