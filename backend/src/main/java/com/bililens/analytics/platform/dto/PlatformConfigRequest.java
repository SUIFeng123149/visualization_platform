package com.bililens.analytics.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record PlatformConfigRequest(
        @NotBlank @Pattern(regexp = "[a-z0-9_-]{2,32}") String platformCode,
        @NotBlank @Size(max = 64) String displayName,
        @NotBlank @Size(max = 128) String connectorName,
        Map<String, Boolean> capabilities,
        boolean enabled
) {
}
