package com.bililens.analytics.crawler;

import com.bililens.analytics.common.ApiResponse;
import tools.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/crawler")
public class CrawlerPlatformController {

    private final CrawlerPlatformProperties properties;
    private final CrawlerPlatformClient client;

    public CrawlerPlatformController(CrawlerPlatformProperties properties, CrawlerPlatformClient client) {
        this.properties = properties;
        this.client = client;
    }

    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status() {
        return ApiResponse.ok(Map.of("configured", properties.configured(), "message", properties.configured() ? "爬虫平台已配置" : "暂未接入爬虫平台，请配置 CRAWLER_BASE_URL"));
    }

    @GetMapping("/auth-profiles")
    public ApiResponse<JsonNode> listAuthProfiles() { return ApiResponse.ok(client.get("/api/v1/auth-profiles")); }

    @PostMapping("/auth-profiles")
    public ApiResponse<JsonNode> createAuthProfile(@Valid @RequestBody AuthProfileRequest request) { return ApiResponse.ok(client.post("/api/v1/auth-profiles", request)); }

    @PostMapping("/auth-profiles/{profileId}/verify")
    public ApiResponse<JsonNode> verifyAuthProfile(@PathVariable String profileId) { return ApiResponse.ok(client.post("/api/v1/auth-profiles/" + profileId + "/verify", Map.of())); }

    @PostMapping("/auth-profiles/{profileId}/enable")
    public ApiResponse<JsonNode> enableAuthProfile(@PathVariable String profileId) { return ApiResponse.ok(client.post("/api/v1/auth-profiles/" + profileId + "/enable", Map.of())); }

    @PostMapping("/auth-profiles/{profileId}/disable")
    public ApiResponse<JsonNode> disableAuthProfile(@PathVariable String profileId) { return ApiResponse.ok(client.post("/api/v1/auth-profiles/" + profileId + "/disable", Map.of())); }

    @PostMapping("/crawl-jobs")
    public ApiResponse<JsonNode> createJob(@Valid @RequestBody CrawlJobRequest request) { return ApiResponse.ok(client.post("/api/v1/crawl-jobs", request)); }

    @GetMapping("/crawl-jobs/{jobId}")
    public ApiResponse<JsonNode> getJob(@PathVariable String jobId) { return ApiResponse.ok(client.get("/api/v1/crawl-jobs/" + jobId)); }

    @PostMapping("/crawl-jobs/{jobId}/cancel")
    public ApiResponse<JsonNode> cancelJob(@PathVariable String jobId) { return ApiResponse.ok(client.post("/api/v1/crawl-jobs/" + jobId + "/cancel", Map.of())); }

    @PostMapping("/crawl-jobs/{jobId}/resume")
    public ApiResponse<JsonNode> resumeJob(@PathVariable String jobId, @RequestBody(required = false) ResumeJobRequest request) { return ApiResponse.ok(client.post("/api/v1/crawl-jobs/" + jobId + "/resume", request == null ? Map.of() : request)); }

    public record AuthProfileRequest(
            @NotBlank String platform,
            @NotBlank String profile_name,
            @NotBlank String profile_directory
    ) { }
    public record CrawlJobRequest(
            @NotBlank String source_url,
            @NotBlank String auth_profile_id,
            @Min(1) @Max(500) Integer video_limit,
            Map<String, Object> strategy
    ) { }
    public record ResumeJobRequest(Map<String, Object> strategy) { }
}
