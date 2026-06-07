package com.bililens.analytics.platform.dto;

import java.time.LocalDateTime;

public record ReportHistoryDto(
        Long id,
        String reportName,
        String reportType,
        String status,
        long rowCount,
        String fileName,
        String remark,
        LocalDateTime createdAt
) {
}
