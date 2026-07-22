package com.bililens.analytics.analysis.controller;

import com.bililens.analytics.analysis.dto.DanmakuTimelineDto;
import com.bililens.analytics.analysis.dto.KeywordTopDto;
import com.bililens.analytics.analysis.dto.NegativeCommentDto;
import com.bililens.analytics.analysis.dto.SentimentTrendDto;
import com.bililens.analytics.analysis.dto.UpPerformanceDto;
import com.bililens.analytics.analysis.dto.VideoDetailDto;
import com.bililens.analytics.analysis.dto.VideoHeatRankDto;
import com.bililens.analytics.analysis.dto.VideoSentimentDto;
import com.bililens.analytics.analysis.service.AnalysisService;
import com.bililens.analytics.common.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.cache.CacheManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/analysis")
@ConditionalOnProperty(prefix = "analytics.legacy", name = "enabled", havingValue = "true")
@Deprecated(since = "0.2.0", forRemoval = false)
public class AnalysisController {

    private final AnalysisService analysisService;
    private final CacheManager cacheManager;

    public AnalysisController(AnalysisService analysisService, CacheManager cacheManager) {
        this.analysisService = analysisService;
        this.cacheManager = cacheManager;
    }

    @GetMapping("/videos/heat-rank")
    public ApiResponse<List<VideoHeatRankDto>> getVideoHeatRank(
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit
    ) {
        return ApiResponse.ok(analysisService.getVideoHeatRank(limit));
    }

    @GetMapping("/videos/historical-samples")
    public ApiResponse<List<VideoHeatRankDto>> getHistoricalVideoSamples(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        return ApiResponse.ok(analysisService.getHistoricalVideoSamples(limit));
    }

    @GetMapping("/videos/sentiment")
    public ApiResponse<List<VideoSentimentDto>> getVideoSentiments(
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit
    ) {
        return ApiResponse.ok(analysisService.getVideoSentiments(limit));
    }

    @GetMapping("/videos/sentiment/by-bvids")
    public ApiResponse<List<VideoSentimentDto>> getVideoSentimentsByBvids(
            @RequestParam @Size(min = 1, max = 100) List<@Size(max = 32) String> bvids
    ) {
        return ApiResponse.ok(analysisService.getVideoSentimentsByBvids(bvids));
    }

    @GetMapping("/videos/{bvid}/detail")
    public ApiResponse<VideoDetailDto> getVideoDetail(@PathVariable @Size(max = 32) String bvid) {
        return ApiResponse.ok(analysisService.getVideoDetail(bvid));
    }

    @GetMapping("/sentiment/trend")
    public ApiResponse<List<SentimentTrendDto>> getSentimentTrend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ApiResponse.ok(analysisService.getSentimentTrend(startDate, endDate));
    }

    @GetMapping("/danmaku/timeline")
    public ApiResponse<List<DanmakuTimelineDto>> getDanmakuTimeline(
            @RequestParam(required = false) @Size(max = 32) String bvid
    ) {
        return ApiResponse.ok(analysisService.getDanmakuTimeline(bvid));
    }

    @GetMapping("/keywords")
    public ApiResponse<List<KeywordTopDto>> getKeywords(
            @RequestParam(defaultValue = "global") @Size(max = 32) String dimensionType,
            @RequestParam(defaultValue = "all") @Size(max = 128) String dimensionValue,
            @RequestParam(defaultValue = "30") @Min(1) @Max(100) int limit
    ) {
        return ApiResponse.ok(analysisService.getKeywords(dimensionType, dimensionValue, limit));
    }

    @GetMapping("/ups/performance")
    public ApiResponse<List<UpPerformanceDto>> getUpPerformance(
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit
    ) {
        return ApiResponse.ok(analysisService.getUpPerformance(limit));
    }

    @GetMapping("/comments/negative")
    public ApiResponse<List<NegativeCommentDto>> getNegativeComments(
            @RequestParam(required = false) @Size(max = 32) String bvid,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        return ApiResponse.ok(analysisService.getNegativeComments(bvid, limit));
    }

    @PostMapping("/cache/evict")
    public ApiResponse<Void> evictCache() {
        for (String name : cacheManager.getCacheNames()) {
            var cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        }
        return ApiResponse.ok(null);
    }

    @Scheduled(initialDelay = 300000, fixedRate = 300000)
    public void scheduledCacheEvict() {
        evictCache();
    }
}
