package com.bililens.analytics.platform.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record ReportCreateRequest(
        @Size(max = 128) String reportName,
        @Size(max = 64) String reportType,
        @Size(max = 255) String fileName,
        @Size(max = 255) String remark,
        @Min(0) @Max(1_000_000) Long rowCount
) {
}
