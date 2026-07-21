package com.bililens.analytics.collector.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CollectorTaskPullRequest(
        @Size(max = 128) String workerId,
        List<String> capabilities,
        @Min(1) @Max(20) Integer limit
) {
}
