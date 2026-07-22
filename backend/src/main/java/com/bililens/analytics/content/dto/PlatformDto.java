package com.bililens.analytics.content.dto;

import java.util.Map;

public record PlatformDto(
        String platformCode,
        String displayName,
        String connectorName,
        Map<String, Boolean> capabilities,
        boolean enabled
) {
}
