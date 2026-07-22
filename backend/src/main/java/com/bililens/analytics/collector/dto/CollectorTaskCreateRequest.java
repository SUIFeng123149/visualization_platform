package com.bililens.analytics.collector.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public record CollectorTaskCreateRequest(
        @Size(max = 120) String taskName,
        @Size(max = 2000) String naturalLanguage,
        @Size(max = 64) String sourceType,
        @Size(max = 64) String collectMode,
        List<String> keywords,
        List<Long> mids,
        Boolean collectPopular,
        Boolean collectHomepage,
        @Min(1) @Max(5000) Integer maxVideos,
        @Min(0) @Max(1000) Integer maxCommentsPerVideo,
        @Min(1) @Max(8) Integer workers,
        @Min(0) @Max(120) Double commentDelay,
        @Min(0) @Max(180) Double danmakuDelay,
        Boolean ocrEnabled,
        Boolean screenCaptureEnabled,
        @Size(max = 32) String ocrLanguage,
        @Min(0) @Max(1) Double minOcrConfidence,
        @Size(max = 16) String priority,
        @Size(max = 500) String callbackUrl,
        @Size(max = 32) String platformCode,
        @Size(max = 128) String connectorName,
        @Size(max = 32) String targetType,
        List<@Size(max = 500) String> targets,
        Map<String, Object> options,
        List<@Size(max = 64) String> requestedCapabilities
) {
}
