package com.bililens.analytics.content.service;

import com.bililens.analytics.content.dto.ContentSummaryDto;
import com.bililens.analytics.content.dto.AccountPerformanceDto;
import com.bililens.analytics.content.dto.CommentInsightSummaryDto;
import com.bililens.analytics.content.dto.CommentTrendPointDto;
import com.bililens.analytics.content.dto.CommentTopicDto;
import com.bililens.analytics.content.dto.ContentMetricHistoryPointDto;
import com.bililens.analytics.content.dto.DashboardSummaryDto;
import com.bililens.analytics.content.dto.InteractionDto;
import com.bililens.analytics.content.dto.InteractionTypeCountDto;
import com.bililens.analytics.content.dto.KeywordDto;
import com.bililens.analytics.content.dto.MetricComparisonDto;
import com.bililens.analytics.content.dto.MetricDefinitionDto;
import com.bililens.analytics.content.dto.PlatformDto;
import com.bililens.analytics.content.dto.SentimentSummaryDto;
import com.bililens.analytics.content.dto.TimelinePointDto;
import com.bililens.analytics.content.dto.TrendPointDto;
import com.bililens.analytics.content.repository.ContentRepository;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.bililens.analytics.content.dto.PagedContentResponse;
import com.bililens.analytics.content.dto.PagedNegativeInteractionResponse;

@Service
public class ContentService {

    private final ContentRepository contentRepository;

    public ContentService(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    public List<PlatformDto> getPlatforms() {
        return contentRepository.findPlatforms();
    }

    public List<ContentSummaryDto> getContents(String platform, String contentType, int limit) {
        return contentRepository.findContents(platform, contentType, limit);
    }

    public DashboardSummaryDto getDashboardSummary(String platform) {
        return contentRepository.findDashboardSummary(platform);
    }

    public PagedContentResponse getContents(String platform, String contentType, String keyword,
                                            LocalDate startDate, LocalDate endDate, int page, int pageSize) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate must be earlier than or equal to endDate");
        }
        int safePage = Math.max(1, page);
        int safePageSize = Math.min(100, Math.max(1, pageSize));
        long total = contentRepository.countContents(platform, contentType, keyword, startDate, endDate);
        return new PagedContentResponse(
                contentRepository.findContents(platform, contentType, keyword, startDate, endDate, safePage, safePageSize),
                safePage, safePageSize, total
        );
    }

    public ContentSummaryDto getContent(long contentId) {
        return contentRepository.findContent(contentId)
                .orElseThrow(() -> new IllegalArgumentException("内容不存在: " + contentId));
    }

    public List<ContentSummaryDto> getChildren(long contentId, int limit) {
        getContent(contentId);
        return contentRepository.findChildren(contentId, limit);
    }

    public List<InteractionDto> getInteractions(long contentId, String interactionType, int limit) {
        getContent(contentId);
        validateInteractionType(interactionType);
        return contentRepository.findInteractions(contentId, interactionType, limit);
    }

    public SentimentSummaryDto getSentiment(long contentId) {
        getContent(contentId);
        return contentRepository.findSentiment(contentId);
    }

    public List<TimelinePointDto> getTimeline(long contentId, String interactionType) {
        getContent(contentId);
        String type = interactionType == null || interactionType.isBlank() ? "danmaku" : interactionType.trim();
        validateInteractionType(type);
        return contentRepository.findTimeline(contentId, type);
    }

    public List<ContentMetricHistoryPointDto> getMetricHistory(long contentId) {
        getContent(contentId);
        return contentRepository.findMetricHistory(contentId);
    }

    public List<TrendPointDto> getTrends(String platform, LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate must be earlier than or equal to endDate");
        }
        return contentRepository.findTrends(
                platform,
                startDate == null ? null : Date.valueOf(startDate),
                endDate == null ? null : Date.valueOf(endDate)
        );
    }

    public List<MetricDefinitionDto> getMetricDefinitions() {
        return contentRepository.findMetricDefinitions();
    }

    public List<MetricComparisonDto> getMetricComparison(String metricKey, String platform, String contentType) {
        if (metricKey == null || metricKey.isBlank()) {
            throw new IllegalArgumentException("请选择指标");
        }
        contentRepository.findMetricDefinition(metricKey.trim())
                .orElseThrow(() -> new IllegalArgumentException("未知指标: " + metricKey));
        return contentRepository.findMetricComparison(metricKey.trim(), platform, contentType);
    }

    public List<KeywordDto> getKeywords(String platform, Long contentId, int limit) {
        if (contentId != null) {
            getContent(contentId);
        }
        Map<String, Long> counts = new HashMap<>();
        for (String keywords : contentRepository.findKeywordTexts(platform, contentId)) {
            for (String word : keywords.split("[,，、\\s]+")) {
                if (!word.isBlank()) {
                    counts.merge(word.trim(), 1L, Long::sum);
                }
            }
        }
        List<Map.Entry<String, Long>> sorted = counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(limit)
                .toList();
        ArrayList<KeywordDto> result = new ArrayList<>();
        for (int index = 0; index < sorted.size(); index++) {
            var entry = sorted.get(index);
            result.add(new KeywordDto(entry.getKey(), entry.getValue(), index + 1));
        }
        return result;
    }

    public List<AccountPerformanceDto> getAccountPerformance(String platform, int limit) {
        return contentRepository.findAccountPerformance(platform, limit);
    }

    public CommentInsightSummaryDto getCommentInsightSummary(String platform, String interactionType,
                                                               LocalDate startDate, LocalDate endDate) {
        validateCommentInsightFilters(interactionType, startDate, endDate);
        return contentRepository.findCommentInsightSummary(
                platform, interactionType, sqlDate(startDate), sqlDate(endDate));
    }

    public List<InteractionTypeCountDto> getCommentInteractionTypes(String platform,
                                                                     LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        return contentRepository.findCommentInteractionTypes(platform, sqlDate(startDate), sqlDate(endDate));
    }

    public List<CommentTrendPointDto> getCommentInsightTrends(String platform, String interactionType,
                                                               LocalDate startDate, LocalDate endDate) {
        validateCommentInsightFilters(interactionType, startDate, endDate);
        return contentRepository.findCommentInsightTrends(
                platform, interactionType, sqlDate(startDate), sqlDate(endDate));
    }

    public List<CommentTopicDto> getCommentTopics(String platform, String interactionType,
                                                   LocalDate startDate, LocalDate endDate, int limit) {
        validateCommentInsightFilters(interactionType, startDate, endDate);
        return contentRepository.findCommentTopics(platform, interactionType, sqlDate(startDate), sqlDate(endDate), limit);
    }

    public PagedNegativeInteractionResponse getNegativeInteractions(String platform, String interactionType,
                                                                      LocalDate startDate, LocalDate endDate,
                                                                      int page, int pageSize) {
        validateCommentInsightFilters(interactionType, startDate, endDate);
        int safePage = Math.max(1, page);
        int safePageSize = Math.min(100, Math.max(1, pageSize));
        Date sqlStartDate = sqlDate(startDate);
        Date sqlEndDate = sqlDate(endDate);
        long total = contentRepository.countNegativeInteractions(
                platform, interactionType, sqlStartDate, sqlEndDate);
        return new PagedNegativeInteractionResponse(
                contentRepository.findNegativeInteractions(
                        platform, interactionType, sqlStartDate, sqlEndDate, safePage, safePageSize),
                safePage, safePageSize, total
        );
    }

    private static void validateCommentInsightFilters(String interactionType,
                                                       LocalDate startDate, LocalDate endDate) {
        if (interactionType != null && !interactionType.isBlank()
                && !List.of("comment", "reply", "review").contains(interactionType)) {
            throw new IllegalArgumentException("评论洞察不支持的互动类型: " + interactionType);
        }
        validateDateRange(startDate, endDate);
    }

    private static void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate must be earlier than or equal to endDate");
        }
    }

    private static Date sqlDate(LocalDate date) {
        return date == null ? null : Date.valueOf(date);
    }

    private static void validateInteractionType(String interactionType) {
        if (interactionType == null || interactionType.isBlank()) {
            return;
        }
        if (!List.of("comment", "danmaku", "review", "reply").contains(interactionType)) {
            throw new IllegalArgumentException("不支持的互动类型: " + interactionType);
        }
    }
}
