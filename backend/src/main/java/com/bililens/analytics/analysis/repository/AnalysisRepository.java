package com.bililens.analytics.analysis.repository;

import com.bililens.analytics.analysis.dto.DanmakuTimelineDto;
import com.bililens.analytics.analysis.dto.KeywordTopDto;
import com.bililens.analytics.analysis.dto.NegativeCommentDto;
import com.bililens.analytics.analysis.dto.SentimentTrendDto;
import com.bililens.analytics.analysis.dto.UpPerformanceDto;
import com.bililens.analytics.analysis.dto.VideoHeatRankDto;
import com.bililens.analytics.analysis.dto.VideoSentimentDto;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class AnalysisRepository {

    private final JdbcClient jdbcClient;

    public AnalysisRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<VideoHeatRankDto> findVideoHeatRank(int limit) {
        return jdbcClient.sql("""
                        select bvid, title, up_name, category, view_count, like_count, coin_count,
                               favorite_count, reply_count, danmaku_count, heat_score
                        from ads_video_heat_rank
                        order by heat_score desc, rank_no, bvid
                        limit :limit
                        """)
                .param("limit", limit)
                .query((rs, rowNum) -> new VideoHeatRankDto(
                        rs.getString("bvid"),
                        rs.getString("title"),
                        rs.getString("up_name"),
                        rs.getString("category"),
                        rs.getLong("view_count"),
                        rs.getLong("like_count"),
                        rs.getLong("coin_count"),
                        rs.getLong("favorite_count"),
                        rs.getLong("reply_count"),
                        rs.getLong("danmaku_count"),
                        rs.getDouble("heat_score"),
                        rowNum + 1
                ))
                .list();
    }

    public Optional<VideoHeatRankDto> findVideoHeatRankByBvid(String bvid) {
        return jdbcClient.sql("""
                        select h.bvid, h.title, h.up_name, h.category, h.view_count, h.like_count, h.coin_count,
                               h.favorite_count, h.reply_count, h.danmaku_count, h.heat_score,
                               (
                                   select count(*) + 1
                                   from ads_video_heat_rank higher
                                   where higher.heat_score > h.heat_score
                               ) as rank_no
                        from ads_video_heat_rank h
                        where h.bvid = :bvid
                        """)
                .param("bvid", bvid)
                .query((rs, rowNum) -> new VideoHeatRankDto(
                        rs.getString("bvid"),
                        rs.getString("title"),
                        rs.getString("up_name"),
                        rs.getString("category"),
                        rs.getLong("view_count"),
                        rs.getLong("like_count"),
                        rs.getLong("coin_count"),
                        rs.getLong("favorite_count"),
                        rs.getLong("reply_count"),
                        rs.getLong("danmaku_count"),
                        rs.getDouble("heat_score"),
                        rs.getInt("rank_no")
                ))
                .optional();
    }

    public List<VideoHeatRankDto> findHistoricalVideoSamples(int limit) {
        return jdbcClient.sql("""
                        select d.bvid, count(*) as sample_count, min(d.created_at) as first_seen_at
                        from dws_text_analysis_detail d
                        where d.bvid is not null
                          and not exists (
                              select 1
                              from ads_video_heat_rank h
                              where h.bvid = d.bvid collate utf8mb4_unicode_ci
                          )
                        group by d.bvid
                        order by first_seen_at desc, sample_count desc
                        limit :limit
                        """)
                .param("limit", limit)
                .query((rs, rowNum) -> historicalVideoSample(
                        rs.getString("bvid"),
                        rs.getLong("sample_count"),
                        rowNum + 1
                ))
                .list();
    }

    public Optional<VideoHeatRankDto> findHistoricalVideoSampleByBvid(String bvid) {
        return jdbcClient.sql("""
                        select bvid, count(*) as sample_count
                        from dws_text_analysis_detail
                        where bvid = :bvid
                        group by bvid
                        """)
                .param("bvid", bvid)
                .query((rs, rowNum) -> historicalVideoSample(
                        rs.getString("bvid"),
                        rs.getLong("sample_count"),
                        999
                ))
                .optional();
    }

    public List<VideoSentimentDto> findVideoSentiments(int limit) {
        return jdbcClient.sql("""
                        select bvid, title, avg_sentiment, positive_count, neutral_count,
                               negative_count, total_count, positive_ratio, negative_ratio
                        from ads_video_sentiment
                        order by total_count desc
                        limit :limit
                        """)
                .param("limit", limit)
                .query((rs, rowNum) -> new VideoSentimentDto(
                        rs.getString("bvid"),
                        rs.getString("title"),
                        rs.getDouble("avg_sentiment"),
                        rs.getLong("positive_count"),
                        rs.getLong("neutral_count"),
                        rs.getLong("negative_count"),
                        rs.getLong("total_count"),
                        rs.getDouble("positive_ratio"),
                        rs.getDouble("negative_ratio")
                ))
                .list();
    }

    public List<VideoSentimentDto> findVideoSentimentsByBvids(List<String> bvids) {
        if (bvids == null || bvids.isEmpty()) {
            return List.of();
        }
        return jdbcClient.sql("""
                        select bvid, title, avg_sentiment, positive_count, neutral_count,
                               negative_count, total_count, positive_ratio, negative_ratio
                        from ads_video_sentiment
                        where bvid in (:bvids)
                        """)
                .param("bvids", bvids)
                .query((rs, rowNum) -> new VideoSentimentDto(
                        rs.getString("bvid"),
                        rs.getString("title"),
                        rs.getDouble("avg_sentiment"),
                        rs.getLong("positive_count"),
                        rs.getLong("neutral_count"),
                        rs.getLong("negative_count"),
                        rs.getLong("total_count"),
                        rs.getDouble("positive_ratio"),
                        rs.getDouble("negative_ratio")
                ))
                .list();
    }

    public Optional<VideoSentimentDto> findVideoSentimentByBvid(String bvid) {
        return jdbcClient.sql("""
                        select bvid, title, avg_sentiment, positive_count, neutral_count,
                               negative_count, total_count, positive_ratio, negative_ratio
                        from ads_video_sentiment
                        where bvid = :bvid
                        """)
                .param("bvid", bvid)
                .query((rs, rowNum) -> new VideoSentimentDto(
                        rs.getString("bvid"),
                        rs.getString("title"),
                        rs.getDouble("avg_sentiment"),
                        rs.getLong("positive_count"),
                        rs.getLong("neutral_count"),
                        rs.getLong("negative_count"),
                        rs.getLong("total_count"),
                        rs.getDouble("positive_ratio"),
                        rs.getDouble("negative_ratio")
                ))
                .optional();
    }

    public Optional<VideoSentimentDto> findHistoricalVideoSentimentByBvid(String bvid) {
        return jdbcClient.sql("""
                        select bvid,
                               avg(sentiment_score) as avg_sentiment,
                               sum(sentiment_label = 'positive') as positive_count,
                               sum(sentiment_label = 'neutral') as neutral_count,
                               sum(sentiment_label = 'negative') as negative_count,
                               count(*) as total_count
                        from dws_text_analysis_detail
                        where bvid = :bvid
                        group by bvid
                        """)
                .param("bvid", bvid)
                .query((rs, rowNum) -> {
                    long total = rs.getLong("total_count");
                    long positive = rs.getLong("positive_count");
                    long negative = rs.getLong("negative_count");
                    return new VideoSentimentDto(
                            rs.getString("bvid"),
                            "历史评论样本：" + rs.getString("bvid"),
                            rs.getDouble("avg_sentiment"),
                            positive,
                            rs.getLong("neutral_count"),
                            negative,
                            total,
                            total > 0 ? (double) positive / total : 0,
                            total > 0 ? (double) negative / total : 0
                    );
                })
                .optional();
    }

    public List<SentimentTrendDto> findSentimentTrend(Date startDate, Date endDate) {
        return jdbcClient.sql("""
                        select stat_date, comment_count, danmaku_count, avg_sentiment, negative_ratio
                        from ads_sentiment_by_date
                        where (:startDate is null or stat_date >= :startDate)
                          and (:endDate is null or stat_date <= :endDate)
                        order by stat_date
                        """)
                .param("startDate", startDate)
                .param("endDate", endDate)
                .query((rs, rowNum) -> new SentimentTrendDto(
                        rs.getDate("stat_date").toLocalDate(),
                        rs.getLong("comment_count"),
                        rs.getLong("danmaku_count"),
                        rs.getDouble("avg_sentiment"),
                        rs.getDouble("negative_ratio")
                ))
                .list();
    }

    public List<DanmakuTimelineDto> findDanmakuTimeline(String bvid) {
        return jdbcClient.sql("""
                        select t.bvid, v.title, t.time_bucket, t.danmaku_count, t.avg_sentiment, t.top_words
                        from ads_danmaku_timeline t
                        left join ads_video_heat_rank v on t.bvid = v.bvid
                        where (:bvid is null or t.bvid = :bvid)
                        order by t.bvid, t.time_bucket
                        """)
                .param("bvid", blankToNull(bvid))
                .query((rs, rowNum) -> new DanmakuTimelineDto(
                        rs.getString("bvid"),
                        rs.getString("title"),
                        rs.getInt("time_bucket"),
                        rs.getLong("danmaku_count"),
                        rs.getDouble("avg_sentiment"),
                        rs.getString("top_words")
                ))
                .list();
    }

    public List<KeywordTopDto> findKeywords(String dimensionType, String dimensionValue, int limit) {
        return jdbcClient.sql("""
                        select word, word_count, rank_no
                        from ads_keyword_top
                        where dimension_type = :dimensionType
                          and dimension_value = :dimensionValue
                        order by rank_no
                        limit :limit
                        """)
                .param("dimensionType", dimensionType)
                .param("dimensionValue", dimensionValue)
                .param("limit", limit)
                .query((rs, rowNum) -> new KeywordTopDto(
                        rs.getString("word"),
                        rs.getLong("word_count"),
                        rs.getInt("rank_no")
                ))
                .list();
    }

    public List<KeywordTopDto> findKeywordsFromTextAnalysis(String bvid, int limit) {
        List<String> keywordTexts = jdbcClient.sql("""
                        select keywords
                        from dws_text_analysis_detail
                        where bvid = :bvid
                          and keywords is not null
                          and keywords <> ''
                        """)
                .param("bvid", bvid)
                .query(String.class)
                .list();

        Map<String, Long> counts = new HashMap<>();
        for (String text : keywordTexts) {
            for (String word : text.split("[,，、\\s]+")) {
                if (!word.isBlank()) {
                    counts.merge(word.trim(), 1L, Long::sum);
                }
            }
        }

        List<Map.Entry<String, Long>> sorted = counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(limit)
                .toList();

        ArrayList<KeywordTopDto> result = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            Map.Entry<String, Long> entry = sorted.get(i);
            result.add(new KeywordTopDto(entry.getKey(), entry.getValue(), i + 1));
        }
        return result;
    }

    public List<UpPerformanceDto> findUpPerformance(int limit) {
        return jdbcClient.sql("""
                        select up_name, video_count, avg_view_count, avg_heat_score, avg_sentiment, total_like_count
                        from ads_up_performance
                        order by avg_heat_score desc
                        limit :limit
                        """)
                .param("limit", limit)
                .query((rs, rowNum) -> new UpPerformanceDto(
                        rs.getString("up_name"),
                        rs.getLong("video_count"),
                        rs.getDouble("avg_view_count"),
                        rs.getDouble("avg_heat_score"),
                        rs.getDouble("avg_sentiment"),
                        rs.getLong("total_like_count")
                ))
                .list();
    }

    public List<NegativeCommentDto> findNegativeComments(String bvid, int limit) {
        return jdbcClient.sql("""
                        select c.bvid, c.rpid, c.user_name, c.clean_content, c.like_count, c.crawled_at, d.sentiment_score
                        from dwd_comment_clean c
                        join dws_text_analysis_detail d on c.rpid = d.source_id
                        where d.source_type = 'comment'
                          and d.sentiment_label = 'negative'
                          and (:bvid is null or c.bvid = :bvid)
                        order by c.like_count desc
                        limit :limit
                        """)
                .param("bvid", blankToNull(bvid))
                .param("limit", limit)
                .query((rs, rowNum) -> new NegativeCommentDto(
                        rs.getString("bvid"),
                        rs.getString("rpid"),
                        rs.getString("user_name"),
                        rs.getString("clean_content"),
                        rs.getLong("like_count"),
                        readLocalDateTime(rs, "crawled_at"),
                        rs.getDouble("sentiment_score")
                ))
                .list();
    }

    public List<VideoHeatRankDto> findVideoHeatRankByCategory(String category, int limit) {
        return jdbcClient.sql("""
                        select bvid, title, up_name, category, view_count, like_count, coin_count,
                               favorite_count, reply_count, danmaku_count, heat_score, rank_no
                        from ads_video_heat_rank
                        where (:category is null or category = :category)
                        order by heat_score desc
                        limit :limit
                        """)
                .param("category", blankToNull(category))
                .param("limit", limit)
                .query((rs, rowNum) -> new VideoHeatRankDto(
                        rs.getString("bvid"),
                        rs.getString("title"),
                        rs.getString("up_name"),
                        rs.getString("category"),
                        rs.getLong("view_count"),
                        rs.getLong("like_count"),
                        rs.getLong("coin_count"),
                        rs.getLong("favorite_count"),
                        rs.getLong("reply_count"),
                        rs.getLong("danmaku_count"),
                        rs.getDouble("heat_score"),
                        rs.getInt("rank_no")
                ))
                .list();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static LocalDateTime readLocalDateTime(ResultSet rs, String column) throws SQLException {
        String raw = rs.getString(column);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            Timestamp timestamp = rs.getTimestamp(column);
            if (timestamp != null) {
                return timestamp.toLocalDateTime();
            }
        } catch (SQLException ignored) {
            // Imported CSV/JSONL samples may use ISO-8601 text, for example 2026-06-02T11:16:29+00:00.
        }
        try {
            return OffsetDateTime.parse(raw, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime();
        } catch (RuntimeException ignored) {
            return LocalDateTime.parse(raw.replace(" ", "T"));
        }
    }

    private static VideoHeatRankDto historicalVideoSample(String bvid, long sampleCount, int rankNo) {
        return new VideoHeatRankDto(
                bvid,
                "历史评论样本：" + bvid,
                "未知UP主",
                "历史样本",
                0,
                0,
                0,
                0,
                sampleCount,
                0,
                0,
                rankNo
        );
    }
}
