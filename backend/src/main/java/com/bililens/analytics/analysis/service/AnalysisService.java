package com.bililens.analytics.analysis.service;

import com.bililens.analytics.analysis.dto.ActionInsightDto;
import com.bililens.analytics.analysis.dto.DanmakuTimelineDto;
import com.bililens.analytics.analysis.dto.KeywordTopDto;
import com.bililens.analytics.analysis.dto.NegativeCommentDto;
import com.bililens.analytics.analysis.dto.SentimentTrendDto;
import com.bililens.analytics.analysis.dto.UpPerformanceDto;
import com.bililens.analytics.analysis.dto.VideoDetailDto;
import com.bililens.analytics.analysis.dto.VideoHeatRankDto;
import com.bililens.analytics.analysis.dto.VideoSentimentDto;
import com.bililens.analytics.analysis.repository.AnalysisRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Cacheable(value = "videoSentiments", key = "#limit")
    public List<VideoSentimentDto> getVideoSentiments(int limit) {
        return analysisRepository.findVideoSentiments(limit);
    }

    @Cacheable(value = "videoSentimentsByBvids", key = "#bvids")
    public List<VideoSentimentDto> getVideoSentimentsByBvids(List<String> bvids) {
        return analysisRepository.findVideoSentimentsByBvids(bvids);
    }

    @Cacheable(value = "sentimentTrend", key = "{#startDate, #endDate}")
    public List<SentimentTrendDto> getSentimentTrend(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            startDate = LocalDate.now().minusDays(365);
            endDate = LocalDate.now();
        }
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
        List<KeywordTopDto> keywords = analysisRepository.findKeywords(dimensionType, dimensionValue, limit);
        if (!keywords.isEmpty() || !"bvid".equalsIgnoreCase(dimensionType)) {
            return keywords;
        }
        return buildKeywordsFromTimeline(analysisRepository.findDanmakuTimeline(dimensionValue), limit);
    }

    @Cacheable(value = "upPerformance", key = "#limit")
    public List<UpPerformanceDto> getUpPerformance(int limit) {
        return analysisRepository.findUpPerformance(limit);
    }

    @Cacheable(value = "negativeComments", key = "{#bvid != null ? #bvid : 'all', #limit}")
    public List<NegativeCommentDto> getNegativeComments(String bvid, int limit) {
        return analysisRepository.findNegativeComments(bvid, limit);
    }

    @Cacheable(value = "videoDetail", key = "#bvid")
    public VideoDetailDto getVideoDetail(String bvid) {
        VideoHeatRankDto video = analysisRepository.findVideoHeatRankByBvid(bvid)
                .orElseThrow(() -> new IllegalArgumentException("video not found: " + bvid));
        VideoSentimentDto sentiment = analysisRepository.findVideoSentimentByBvid(bvid).orElse(null);
        List<DanmakuTimelineDto> timeline = analysisRepository.findDanmakuTimeline(bvid);
        List<KeywordTopDto> keywords = analysisRepository.findKeywords("bvid", bvid, 12);
        if (keywords.isEmpty()) {
            keywords = buildKeywordsFromTimeline(timeline, 12);
        }
        List<NegativeCommentDto> negativeComments = analysisRepository.findNegativeComments(bvid, 10);
        List<ActionInsightDto> insights = buildVideoInsights(video, sentiment, timeline, keywords, negativeComments);

        return new VideoDetailDto(video, sentiment, timeline, keywords, negativeComments, insights);
    }

    private static List<ActionInsightDto> buildVideoInsights(
            VideoHeatRankDto video,
            VideoSentimentDto sentiment,
            List<DanmakuTimelineDto> timeline,
            List<KeywordTopDto> keywords,
            List<NegativeCommentDto> negativeComments
    ) {
        List<ActionInsightDto> insights = new ArrayList<>();
        double interactionRate = video.viewCount() > 0
                ? (double) (video.likeCount() + video.coinCount() + video.favoriteCount() + video.replyCount() + video.danmakuCount()) / video.viewCount()
                : 0;
        double negativeRatio = sentiment == null ? 0 : sentiment.negativeRatio();
        double positiveRatio = sentiment == null ? 0 : sentiment.positiveRatio();

        if (video.rankNo() <= 3 && positiveRatio >= 0.55) {
            insights.add(new ActionInsightDto(
                    "爆款潜力高",
                    "增长",
                    "success",
                    "当前视频位于热度榜前列，且正向评论占比较高。",
                    "沉淀标题、封面、选题和发布时间特征，纳入后续选题模板。"
            ));
        }

        if (negativeRatio >= 0.25 || negativeComments.size() >= 2) {
            insights.add(new ActionInsightDto(
                    "舆情风险需处理",
                    "高优先级",
                    "danger",
                    "负向评论占比或负面样本数量偏高，可能影响内容口碑。",
                    "优先查看高赞负面评论，补充置顶解释、评论区回复或二次剪辑说明。"
            ));
        }

        timeline.stream()
                .max(Comparator.comparingLong(DanmakuTimelineDto::danmakuCount))
                .ifPresent(hotspot -> insights.add(new ActionInsightDto(
                        "推荐高能切片",
                        "可执行",
                        "warning",
                        "弹幕峰值出现在 " + formatVideoTime(hotspot.timeBucket()) + "，说明观众集中反应强。",
                        "回看该时间点前后 15 秒，制作短切片或复盘该段内容设计。"
                )));

        if (interactionRate >= 0.08 && negativeRatio < 0.2) {
            insights.add(new ActionInsightDto(
                    "适合二次分发",
                    "增长",
                    "primary",
                    "互动率较高且负向反馈可控，内容具备扩散条件。",
                    "将该视频拆成图文、短视频或社群话题，扩大曝光。"
            ));
        }

        if (keywords.isEmpty()) {
            insights.add(new ActionInsightDto(
                    "关键词样本不足",
                    "分析",
                    "info",
                    "当前视频缺少关键词沉淀，难以判断主要讨论主题。",
                    "补充评论和弹幕清洗分析后，再做主题归因。"
            ));
        } else {
            insights.add(new ActionInsightDto(
                    "主题可归因",
                    "分析",
                    "primary",
                    "高频词集中在 " + keywords.stream().limit(3).map(KeywordTopDto::word).reduce((a, b) -> a + "、" + b).orElse("--") + "。",
                    "围绕这些关键词拆解观众关注点，判断是内容亮点、疑问还是争议。"
            ));
        }

        return insights;
    }

    private static String formatVideoTime(int seconds) {
        return seconds / 60 + ":" + String.format("%02d", seconds % 60);
    }

    private static List<KeywordTopDto> buildKeywordsFromTimeline(List<DanmakuTimelineDto> timeline, int limit) {
        Map<String, Long> counts = new HashMap<>();
        for (DanmakuTimelineDto item : timeline) {
            if (item.topWords() == null || item.topWords().isBlank()) {
                continue;
            }
            for (String word : item.topWords().split("[,，、\\s]+")) {
                if (word.isBlank()) {
                    continue;
                }
                counts.merge(word.trim(), 1L, Long::sum);
            }
        }

        List<Map.Entry<String, Long>> sorted = counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(limit)
                .toList();

        List<KeywordTopDto> keywords = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            Map.Entry<String, Long> entry = sorted.get(i);
            keywords.add(new KeywordTopDto(entry.getKey(), entry.getValue(), i + 1));
        }
        return keywords;
    }

    private static Date toSqlDate(LocalDate date) {
        return date == null ? null : Date.valueOf(date);
    }
}
