package com.bililens.analytics.analysis.controller;

import com.bililens.analytics.analysis.dto.DanmakuTimelineDto;
import com.bililens.analytics.analysis.dto.KeywordTopDto;
import com.bililens.analytics.analysis.dto.NegativeCommentDto;
import com.bililens.analytics.analysis.dto.SentimentTrendDto;
import com.bililens.analytics.analysis.dto.UpPerformanceDto;
import com.bililens.analytics.analysis.dto.VideoHeatRankDto;
import com.bililens.analytics.analysis.dto.VideoSentimentDto;
import com.bililens.analytics.analysis.service.AnalysisService;
import com.bililens.analytics.common.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @GetMapping("/videos/heat-rank")
    public ApiResponse<List<VideoHeatRankDto>> getVideoHeatRank(
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit
    ) {
        return ApiResponse.ok(analysisService.getVideoHeatRank(limit));
    }

    @GetMapping("/videos/sentiment")
    public ApiResponse<List<VideoSentimentDto>> getVideoSentiments() {
        return ApiResponse.ok(analysisService.getVideoSentiments());
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
}
