package com.bililens.analytics.platform.dto;

import java.time.LocalDateTime;

public record DataSourceStatusDto(
        String tableName,
        String displayName,
        String layer,
        long rowCount,
        LocalDateTime latestAt,
        String status,
        String message
) {
}
