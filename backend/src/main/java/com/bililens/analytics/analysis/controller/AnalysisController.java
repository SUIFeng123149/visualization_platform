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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "数据分析", description = "B站内容数据分析相关接口")
@Validated
@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @Operation(summary = "视频热度排行", description = "获取按热度排序的视频列表，支持指定返回数量")
    @GetMapping("/videos/heat-rank")
    public ApiResponse<List<VideoHeatRankDto>> getVideoHeatRank(
            @Parameter(description = "返回数量，范围 1-100") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit
    ) {
        return ApiResponse.ok(analysisService.getVideoHeatRank(limit));
    }

    @Operation(summary = "视频情感统计", description = "获取所有视频的正向/中性/负向情感占比统计")
    @GetMapping("/videos/sentiment")
    public ApiResponse<List<VideoSentimentDto>> getVideoSentiments() {
        return ApiResponse.ok(analysisService.getVideoSentiments());
    }

    @Operation(summary = "情感趋势", description = "按日期范围获取评论情感趋势，支持可选起止日期过滤")
    @GetMapping("/sentiment/trend")
    public ApiResponse<List<SentimentTrendDto>> getSentimentTrend(
            @Parameter(description = "起始日期（可选）") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期（可选）") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ApiResponse.ok(analysisService.getSentimentTrend(startDate, endDate));
    }

    @Operation(summary = "弹幕时间轴", description = "获取视频弹幕按时间片段的分布统计，支持按BVID过滤")
    @GetMapping("/danmaku/timeline")
    public ApiResponse<List<DanmakuTimelineDto>> getDanmakuTimeline(
            @Parameter(description = "视频BVID（可选，不传返回全部）") @RequestParam(required = false) @Size(max = 32) String bvid
    ) {
        return ApiResponse.ok(analysisService.getDanmakuTimeline(bvid));
    }

    @Operation(summary = "关键词排行", description = "获取全局或按视频维度的关键词TOP N排行")
    @GetMapping("/keywords")
    public ApiResponse<List<KeywordTopDto>> getKeywords(
            @Parameter(description = "维度类型：global/bvid") @RequestParam(defaultValue = "global") @Size(max = 32) String dimensionType,
            @Parameter(description = "维度值：all 或具体BVID") @RequestParam(defaultValue = "all") @Size(max = 128) String dimensionValue,
            @Parameter(description = "返回数量，范围 1-100") @RequestParam(defaultValue = "30") @Min(1) @Max(100) int limit
    ) {
        return ApiResponse.ok(analysisService.getKeywords(dimensionType, dimensionValue, limit));
    }

    @Operation(summary = "UP主表现", description = "获取UP主的能力排行，包括播放、热度、口碑和互动数据")
    @GetMapping("/ups/performance")
    public ApiResponse<List<UpPerformanceDto>> getUpPerformance(
            @Parameter(description = "返回数量，范围 1-100") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit
    ) {
        return ApiResponse.ok(analysisService.getUpPerformance(limit));
    }

    @Operation(summary = "负面评论", description = "获取负面情感评论样本，支持按BVID过滤，按点赞数排序")
    @GetMapping("/comments/negative")
    public ApiResponse<List<NegativeCommentDto>> getNegativeComments(
            @Parameter(description = "视频BVID（可选，不传返回全部）") @RequestParam(required = false) @Size(max = 32) String bvid,
            @Parameter(description = "返回数量，范围 1-100") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        return ApiResponse.ok(analysisService.getNegativeComments(bvid, limit));
    }
}
