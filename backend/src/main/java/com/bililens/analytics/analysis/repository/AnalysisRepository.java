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
import java.util.List;
import java.util.Optional;

@Repository
public class AnalysisRepository {

    private final JdbcClient jdbcClient;

    public AnalysisRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /**
     * NOTE: The {@code :param IS NULL OR column = :param} pattern used in several queries
     * below may cause parameter sniffing issues in MySQL under high data volumes.
     * For production scale-up, consider building dynamic SQL in the Service layer
     * or using UNION ALL with separate branches for null and non-null parameters.
     */

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
                        rs.getTimestamp("crawled_at").toLocalDateTime(),
                        rs.getDouble("sentiment_score")
                ))
                .list();
    }

    /**
     * Find top videos by category, ordered by heat score.
     * Used by Dify AI assistant for video recommendation.
     */
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
}
