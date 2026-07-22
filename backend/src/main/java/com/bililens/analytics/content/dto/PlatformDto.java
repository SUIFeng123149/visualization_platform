package com.bililens.analytics.content.dto;

import java.util.Map;

public record PlatformDto(
        String platformCode,
        String displayName,
        Map<String, Boolean> capabilities,
        boolean enabled
) {
}
