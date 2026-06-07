package com.bililens.analytics.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TaskStatusUpdateRequest(
        @NotBlank
        @Size(max = 32)
        @Pattern(regexp = "todo|doing|done|ignored", message = "status must be one of todo, doing, done, ignored")
        String status
) {
}
