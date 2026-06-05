package com.bililens.analytics.analysis.service;

import com.bililens.analytics.analysis.dto.DanmakuTimelineDto;
import com.bililens.analytics.analysis.dto.KeywordTopDto;
import com.bililens.analytics.analysis.dto.NegativeCommentDto;
import com.bililens.analytics.analysis.dto.SentimentTrendDto;
import com.bililens.analytics.analysis.dto.UpPerformanceDto;
import com.bililens.analytics.analysis.dto.VideoHeatRankDto;
import com.bililens.analytics.analysis.dto.VideoSentimentDto;
import com.bililens.analytics.analysis.repository.AnalysisRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Service
public class AnalysisService {

    private final AnalysisRepository analysisRepository;

    public AnalysisService(AnalysisRepository analysisRepository) {
        this.analysisRepository = analysisRepository;
    }

    @Cacheable(value = "videoHeatRank", key = "#limit")
    public List<VideoHeatRankDto> getVideoHeatRank(int limit) {
        return analysisRepository.findVideoHeatRank(limit);
    }

    @Cacheable("videoSentiments")
    public List<VideoSentimentDto> getVideoSentiments() {
        return analysisRepository.findVideoSentiments();
    }

    @Cacheable(value = "sentimentTrend", key = "{#startDate, #endDate}")
    public List<SentimentTrendDto> getSentimentTrend(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate must be earlier than or equal to endDate");
        }
        return analysisRepository.findSentimentTrend(toSqlDate(startDate), toSqlDate(endDate));
    }

    @Cacheable(value = "danmakuTimeline", key = "#bvid != null ? #bvid : 'all'")
    public List<DanmakuTimelineDto> getDanmakuTimeline(String bvid) {
        return analysisRepository.findDanmakuTimeline(bvid);
    }

    @Cacheable(value = "keywords", key = "{#dimensionType, #dimensionValue, #limit}")
    public List<KeywordTopDto> getKeywords(String dimensionType, String dimensionValue, int limit) {
        return analysisRepository.findKeywords(dimensionType, dimensionValue, limit);
    }

    @Cacheable(value = "upPerformance", key = "#limit")
    public List<UpPerformanceDto> getUpPerformance(int limit) {
        return analysisRepository.findUpPerformance(limit);
    }

    @Cacheable(value = "negativeComments", key = "{#bvid != null ? #bvid : 'all', #limit}")
    public List<NegativeCommentDto> getNegativeComments(String bvid, int limit) {
        return analysisRepository.findNegativeComments(bvid, limit);
    }

    @Cacheable(value = "videoHeatRankByCategory", key = "{#category, #limit}")
    public List<VideoHeatRankDto> getVideosByCategory(String category, int limit) {
        return analysisRepository.findVideoHeatRankByCategory(category, limit);
    }

    private static Date toSqlDate(LocalDate date) {
        return date == null ? null : Date.valueOf(date);
    }
}
