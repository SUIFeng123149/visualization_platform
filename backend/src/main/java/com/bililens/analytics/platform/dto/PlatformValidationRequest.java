package com.bililens.analytics.platform.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PlatformValidationRequest(
        @NotEmpty @Size(max = 100) List<@Pattern(regexp = "[a-z0-9_-]{2,32}") String> platformCodes
) {
}
