package com.bililens.analytics.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MetricConfigRequest(
        @NotBlank @Size(max = 128) String displayName,
        @Size(max = 32) String unit,
        @NotBlank @Size(max = 32) String scope,
        @Size(max = 2000) String definition,
        boolean comparable
) {
}
