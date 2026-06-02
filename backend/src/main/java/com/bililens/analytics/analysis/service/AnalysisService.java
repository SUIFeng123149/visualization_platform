package com.bililens.analytics.analysis.service;

import com.bililens.analytics.analysis.dto.DanmakuTimelineDto;
import com.bililens.analytics.analysis.dto.KeywordTopDto;
import com.bililens.analytics.analysis.dto.NegativeCommentDto;
import com.bililens.analytics.analysis.dto.SentimentTrendDto;
import com.bililens.analytics.analysis.dto.UpPerformanceDto;
import com.bililens.analytics.analysis.dto.VideoHeatRankDto;
import com.bililens.analytics.analysis.dto.VideoSentimentDto;
import com.bililens.analytics.analysis.repository.AnalysisRepository;
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

    public List<VideoHeatRankDto> getVideoHeatRank(int limit) {
        return analysisRepository.findVideoHeatRank(limit);
    }

    public List<VideoSentimentDto> getVideoSentiments() {
        return analysisRepository.findVideoSentiments();
    }

    public List<SentimentTrendDto> getSentimentTrend(LocalDate startDate, LocalDate endDate) {
        return analysisRepository.findSentimentTrend(toSqlDate(startDate), toSqlDate(endDate));
    }

    public List<DanmakuTimelineDto> getDanmakuTimeline(String bvid) {
        return analysisRepository.findDanmakuTimeline(bvid);
    }

    public List<KeywordTopDto> getKeywords(String dimensionType, String dimensionValue, int limit) {
        return analysisRepository.findKeywords(dimensionType, dimensionValue, limit);
    }

    public List<UpPerformanceDto> getUpPerformance(int limit) {
        return analysisRepository.findUpPerformance(limit);
    }

    public List<NegativeCommentDto> getNegativeComments(String bvid, int limit) {
        return analysisRepository.findNegativeComments(bvid, limit);
    }

    private static Date toSqlDate(LocalDate date) {
        return date == null ? null : Date.valueOf(date);
    }
}
