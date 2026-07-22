package com.bililens.analytics.platform.dto;

import java.util.Map;

public record PlatformConfigDto(
        String platformCode,
        String displayName,
        String connectorName,
        Map<String, Boolean> capabilities,
        boolean enabled
) {
}
