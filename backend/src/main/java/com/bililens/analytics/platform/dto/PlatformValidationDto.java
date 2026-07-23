package com.bililens.analytics.platform.dto;

import java.util.List;

public record PlatformValidationDto(
        List<String> configuredPlatformCodes,
        List<String> disabledPlatformCodes,
        List<String> missingPlatformCodes,
        boolean valid
) {
}
