package com.bililens.analytics.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordConfirmRequest(@NotBlank @Size(max = 256) String password) {
}
