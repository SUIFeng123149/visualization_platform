package com.bililens.analytics.content.controller;

import com.bililens.analytics.common.ApiResponse;
import com.bililens.analytics.content.dto.ContentSummaryDto;
import com.bililens.analytics.content.dto.AccountPerformanceDto;
import com.bililens.analytics.content.dto.CommentInsightSummaryDto;
import com.bililens.analytics.content.dto.CommentTrendPointDto;
import com.bililens.analytics.content.dto.ContentMetricHistoryPointDto;
import com.bililens.analytics.content.dto.InteractionDto;
import com.bililens.analytics.content.dto.InteractionTypeCountDto;
import com.bililens.analytics.content.dto.KeywordDto;
import com.bililens.analytics.content.dto.MetricComparisonDto;
import com.bililens.analytics.content.dto.MetricDefinitionDto;
import com.bililens.analytics.content.dto.PlatformDto;
import com.bililens.analytics.content.dto.PagedNegativeInteractionResponse;
import com.bililens.analytics.content.dto.SentimentSummaryDto;
import com.bililens.analytics.content.dto.TimelinePointDto;
import com.bililens.analytics.content.dto.TrendPointDto;
import com.bililens.analytics.content.service.ContentService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.time.LocalDate;

@Validated
@RestController
@RequestMapping("/api/v2")
public class ContentController {

    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping("/platforms")
    public ApiResponse<List<PlatformDto>> getPlatforms() {
        return ApiResponse.ok(contentService.getPlatforms());
    }

    @GetMapping("/contents")
    public ApiResponse<List<ContentSummaryDto>> getContents(
            @RequestParam(required = false) @Size(max = 32) String platform,
            @RequestParam(required = false) @Size(max = 32) String contentType,
            @RequestParam(defaultValue = "20") @Min(1) @Max(500) int limit
    ) {
        return ApiResponse.ok(contentService.getContents(platform, contentType, limit));
    }

    @GetMapping("/contents/page")
    public ApiResponse<com.bililens.analytics.content.dto.PagedContentResponse> getContentsPage(
            @RequestParam(required = false) @Size(max = 32) String platform,
            @RequestParam(required = false) @Size(max = 32) String contentType,
            @RequestParam(required = false) @Size(max = 120) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize
    ) {
        return ApiResponse.ok(contentService.getContents(platform, contentType, keyword, startDate, endDate, page, pageSize));
    }

    @GetMapping("/contents/{contentId}")
    public ApiResponse<ContentSummaryDto> getContent(@PathVariable @Min(1) long contentId) {
        return ApiResponse.ok(contentService.getContent(contentId));
    }

    @GetMapping("/contents/{contentId}/children")
    public ApiResponse<List<ContentSummaryDto>> getChildren(
            @PathVariable @Min(1) long contentId,
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit
    ) {
        return ApiResponse.ok(contentService.getChildren(contentId, limit));
    }

    @GetMapping("/contents/{contentId}/interactions")
    public ApiResponse<List<InteractionDto>> getInteractions(
            @PathVariable @Min(1) long contentId,
            @RequestParam(required = false) @Size(max = 32) String type,
            @RequestParam(defaultValue = "50") @Min(1) @Max(500) int limit
    ) {
        return ApiResponse.ok(contentService.getInteractions(contentId, type, limit));
    }

    @GetMapping("/contents/{contentId}/sentiment")
    public ApiResponse<SentimentSummaryDto> getSentiment(@PathVariable @Min(1) long contentId) {
        return ApiResponse.ok(contentService.getSentiment(contentId));
    }

    @GetMapping("/contents/{contentId}/timeline")
    public ApiResponse<List<TimelinePointDto>> getTimeline(
            @PathVariable @Min(1) long contentId,
            @RequestParam(defaultValue = "danmaku") @Size(max = 32) String type
    ) {
        return ApiResponse.ok(contentService.getTimeline(contentId, type));
    }

    @GetMapping("/contents/{contentId}/metric-history")
    public ApiResponse<List<ContentMetricHistoryPointDto>> getMetricHistory(@PathVariable @Min(1) long contentId) {
        return ApiResponse.ok(contentService.getMetricHistory(contentId));
    }

    @GetMapping("/analytics/trends")
    public ApiResponse<List<TrendPointDto>> getTrends(
            @RequestParam(required = false) @Size(max = 32) String platform,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ApiResponse.ok(contentService.getTrends(platform, startDate, endDate));
    }

    @GetMapping("/analytics/keywords")
    public ApiResponse<List<KeywordDto>> getKeywords(
            @RequestParam(required = false) @Size(max = 32) String platform,
            @RequestParam(required = false) @Min(1) Long contentId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        return ApiResponse.ok(contentService.getKeywords(platform, contentId, limit));
    }

    @GetMapping("/analytics/metric-definitions")
    public ApiResponse<List<MetricDefinitionDto>> getMetricDefinitions() {
        return ApiResponse.ok(contentService.getMetricDefinitions());
    }

    @GetMapping("/analytics/metric-comparison")
    public ApiResponse<List<MetricComparisonDto>> getMetricComparison(
            @RequestParam @Size(max = 64) String metricKey,
            @RequestParam(required = false) @Size(max = 32) String platform,
            @RequestParam(required = false) @Size(max = 32) String contentType
    ) {
        return ApiResponse.ok(contentService.getMetricComparison(metricKey, platform, contentType));
    }

    @GetMapping("/analytics/comments/summary")
    public ApiResponse<CommentInsightSummaryDto> getCommentInsightSummary(
            @RequestParam(required = false) @Size(max = 32) String platform,
            @RequestParam(required = false) @Size(max = 32) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ApiResponse.ok(contentService.getCommentInsightSummary(platform, type, startDate, endDate));
    }

    @GetMapping("/analytics/comments/types")
    public ApiResponse<List<InteractionTypeCountDto>> getCommentInteractionTypes(
            @RequestParam(required = false) @Size(max = 32) String platform,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ApiResponse.ok(contentService.getCommentInteractionTypes(platform, startDate, endDate));
    }

    @GetMapping("/analytics/comments/trends")
    public ApiResponse<List<CommentTrendPointDto>> getCommentInsightTrends(
            @RequestParam(required = false) @Size(max = 32) String platform,
            @RequestParam(required = false) @Size(max = 32) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ApiResponse.ok(contentService.getCommentInsightTrends(platform, type, startDate, endDate));
    }

    @GetMapping("/analytics/comments/negative")
    public ApiResponse<PagedNegativeInteractionResponse> getNegativeInteractions(
            @RequestParam(required = false) @Size(max = 32) String platform,
            @RequestParam(required = false) @Size(max = 32) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize
    ) {
        return ApiResponse.ok(contentService.getNegativeInteractions(
                platform, type, startDate, endDate, page, pageSize));
    }

    @GetMapping("/accounts/performance")
    public ApiResponse<List<AccountPerformanceDto>> getAccountPerformance(
            @RequestParam(required = false) @Size(max = 32) String platform,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        return ApiResponse.ok(contentService.getAccountPerformance(platform, limit));
    }
}
