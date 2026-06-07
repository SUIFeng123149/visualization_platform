package com.bililens.analytics.platform.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnomalyRuleUpdateRequest(
        @NotBlank @Size(max = 64) String ruleKey,
        @NotBlank @Size(max = 128) String name,
        @NotBlank @Size(max = 64) String metric,
        @NotBlank @Size(max = 16) String operator,
        @Min(0) @Max(1_000_000) double threshold,
        @NotBlank @Size(max = 32) String level,
        boolean enabled,
        @Size(max = 255) String description
) {
}
