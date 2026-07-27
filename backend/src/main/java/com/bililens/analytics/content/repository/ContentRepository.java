package com.bililens.analytics.content.repository;

import com.bililens.analytics.content.dto.ContentSummaryDto;
import com.bililens.analytics.content.dto.AccountPerformanceDto;
import com.bililens.analytics.content.dto.CommentInsightSummaryDto;
import com.bililens.analytics.content.dto.CommentTrendPointDto;
import com.bililens.analytics.content.dto.CommentTopicDto;
import com.bililens.analytics.content.dto.ContentMetricHistoryPointDto;
import com.bililens.analytics.content.dto.DashboardSummaryDto;
import com.bililens.analytics.content.dto.InteractionDto;
import com.bililens.analytics.content.dto.InteractionTypeCountDto;
import com.bililens.analytics.content.dto.MetricComparisonDto;
import com.bililens.analytics.content.dto.MetricDefinitionDto;
import com.bililens.analytics.content.dto.NegativeInteractionDto;
import com.bililens.analytics.content.dto.PlatformDto;
import com.bililens.analytics.content.dto.SentimentSummaryDto;
import com.bililens.analytics.content.dto.TimelinePointDto;
import com.bililens.analytics.content.dto.TrendPointDto;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Set;

@Repository
public class ContentRepository {

    private static final Set<String> TOPIC_STOP_WORDS = Set.of(
            "的", "了", "是", "有", "和", "都", "就", "也", "这", "那", "一个", "什么", "真的", "感觉", "可以", "视频", "内容"
    );
    private static final Map<String, String> TOPIC_SYNONYMS = Map.ofEntries(
            Map.entry("好看", "正向评价"), Map.entry("不错", "正向评价"), Map.entry("喜欢", "正向评价"), Map.entry("推荐", "正向评价"),
            Map.entry("垃圾", "负面评价"), Map.entry("烂", "负面评价"), Map.entry("失望", "负面评价"), Map.entry("难看", "负面评价"),
            Map.entry("画质", "画面质量"), Map.entry("特效", "画面质量"),
            Map.entry("配音", "音频表现"), Map.entry("音乐", "音频表现")
    );

    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;

    public ContentRepository(JdbcClient jdbcClient, ObjectMapper objectMapper) {
        this.jdbcClient = jdbcClient;
        this.objectMapper = objectMapper;
    }

    public List<PlatformDto> findPlatforms() {
        try {
            return jdbcClient.sql("""
                        select platform_code, display_name, connector_name, capabilities_json, enabled
                        from dim_platform
                        order by platform_code
                        """)
                    .query((rs, rowNum) -> new PlatformDto(
                            rs.getString("platform_code"),
                            rs.getString("display_name"),
                            rs.getString("connector_name"),
                            readCapabilities(rs.getString("capabilities_json")),
                            rs.getBoolean("enabled")
                    ))
                    .list();
        } catch (DataAccessException ignored) {
            return jdbcClient.sql("""
                            select platform_code, display_name, capabilities_json, enabled
                            from dim_platform
                            order by platform_code
                            """)
                .query((rs, rowNum) -> new PlatformDto(
                        rs.getString("platform_code"),
                        rs.getString("display_name"),
                        rs.getString("platform_code") + "-default",
                        readCapabilities(rs.getString("capabilities_json")),
                        rs.getBoolean("enabled")
                ))
                .list();
        }
    }

    public List<ContentSummaryDto> findContents(String platform, String contentType, int limit) {
        return jdbcClient.sql("""
                        select c.content_id, c.platform_code, c.external_content_id, c.content_type,
                               c.parent_content_id, c.title, a.display_name as account_name, c.category,
                               c.published_at, m.view_count, m.like_count, m.comment_count, m.share_count,
                               m.favorite_count, m.danmaku_count, m.coin_count, m.platform_heat_score,
                               m.normalized_heat_score, m.captured_at
                        from dim_content c
                        left join dim_account a on a.account_id = c.account_id
                        left join fact_content_metric_snapshot m on m.snapshot_id = (
                            select latest.snapshot_id from fact_content_metric_snapshot latest
                            where latest.content_id = c.content_id
                            order by latest.captured_at desc, latest.snapshot_id desc limit 1
                        )
                        where (:platform is null or c.platform_code = :platform)
                          and (:contentType is null or c.content_type = :contentType)
                        order by coalesce(m.normalized_heat_score, -1) desc, c.content_id desc
                        limit :limit
                        """)
                .param("platform", blankToNull(platform))
                .param("contentType", blankToNull(contentType))
                .param("limit", limit)
                .query((rs, rowNum) -> toSummary(rs))
                .list();
    }

    public DashboardSummaryDto findDashboardSummary(String platform) {
        return jdbcClient.sql("""
                        select count(distinct c.content_id) as content_count,
                               count(distinct c.platform_code) as platform_count,
                               coalesce(sum(m.view_count), 0) as total_views,
                               avg(m.normalized_heat_score) as average_normalized_heat,
                               max(m.captured_at) as latest_captured_at,
                               (
                                   select count(*) from fact_interaction i
                                   join dim_content interaction_content on interaction_content.content_id = i.content_id
                                   where (:platform is null or interaction_content.platform_code = :platform)
                               ) as interaction_count
                        from dim_content c
                        left join fact_content_metric_snapshot m on m.snapshot_id = (
                            select latest.snapshot_id from fact_content_metric_snapshot latest
                            where latest.content_id = c.content_id
                            order by latest.captured_at desc, latest.snapshot_id desc limit 1
                        )
                        where (:platform is null or c.platform_code = :platform)
                        """)
                .param("platform", blankToNull(platform))
                .query((rs, rowNum) -> new DashboardSummaryDto(
                        rs.getLong("content_count"), rs.getLong("platform_count"), rs.getLong("total_views"),
                        nullableDouble(rs, "average_normalized_heat"), rs.getLong("interaction_count"),
                        localDateTime(rs, "latest_captured_at")
                ))
                .single();
    }

    public List<MetricDefinitionDto> findMetricDefinitions() {
        return jdbcClient.sql("""
                        select metric_key, display_name, unit, scope, definition, comparable
                        from metric_dictionary
                        order by comparable desc, display_name
                        """)
                .query((rs, rowNum) -> toMetricDefinition(rs))
                .list();
    }

    public List<ContentMetricHistoryPointDto> findMetricHistory(long contentId) {
        return jdbcClient.sql("""
                        select captured_at, view_count, like_count, comment_count, share_count, normalized_heat_score
                        from fact_content_metric_snapshot
                        where content_id = :contentId
                        order by captured_at, snapshot_id
                        """)
                .param("contentId", contentId)
                .query((rs, rowNum) -> new ContentMetricHistoryPointDto(
                        localDateTime(rs, "captured_at"), nullableLong(rs, "view_count"),
                        nullableLong(rs, "like_count"), nullableLong(rs, "comment_count"),
                        nullableLong(rs, "share_count"), nullableDouble(rs, "normalized_heat_score")
                ))
                .list();
    }

    public Optional<MetricDefinitionDto> findMetricDefinition(String metricKey) {
        return jdbcClient.sql("""
                        select metric_key, display_name, unit, scope, definition, comparable
                        from metric_dictionary
                        where metric_key = :metricKey
                        """)
                .param("metricKey", metricKey)
                .query((rs, rowNum) -> toMetricDefinition(rs))
                .optional();
    }

    public List<MetricComparisonDto> findMetricComparison(String metricKey, String platform, String contentType) {
        String expression = metricExpression(metricKey);
        if (expression == null) {
            return findExtraMetricComparison(metricKey, platform, contentType);
        }
        String sql = """
                select c.platform_code, count(distinct c.content_id) as content_count,
                       count(%1$s) as available_count, max(m.captured_at) as latest_captured_at, avg(%1$s) as average_value,
                       min(%1$s) as min_value, max(%1$s) as max_value
                from dim_content c
                left join fact_content_metric_snapshot m on m.snapshot_id = (
                    select latest.snapshot_id from fact_content_metric_snapshot latest
                    where latest.content_id = c.content_id
                    order by latest.captured_at desc, latest.snapshot_id desc limit 1
                )
                where (:platform is null or c.platform_code = :platform)
                  and (:contentType is null or c.content_type = :contentType)
                group by c.platform_code
                order by average_value desc, c.platform_code
                """.formatted(expression);
        return jdbcClient.sql(sql)
                .param("platform", blankToNull(platform))
                .param("contentType", blankToNull(contentType))
                .query((rs, rowNum) -> new MetricComparisonDto(
                        metricKey, rs.getString("platform_code"), rs.getLong("content_count"),
                        rs.getLong("available_count"), nullableDouble(rs, "average_value"),
                        nullableDouble(rs, "min_value"), nullableDouble(rs, "max_value"),
                        localDateTime(rs, "latest_captured_at")
                ))
                .list();
    }

    /** Connector-specific values are queried only by the standalone metric explorer. */
    private List<MetricComparisonDto> findExtraMetricComparison(String metricKey, String platform, String contentType) {
        Map<String, MetricAccumulator> accumulators = new LinkedHashMap<>();
        jdbcClient.sql("""
                        select c.platform_code, m.extra_metrics, m.captured_at
                        from dim_content c
                        left join fact_content_metric_snapshot m on m.snapshot_id = (
                            select latest.snapshot_id from fact_content_metric_snapshot latest
                            where latest.content_id = c.content_id
                            order by latest.captured_at desc, latest.snapshot_id desc limit 1
                        )
                        where (:platform is null or c.platform_code = :platform)
                          and (:contentType is null or c.content_type = :contentType)
                        order by c.platform_code, c.content_id
                        """)
                .param("platform", blankToNull(platform))
                .param("contentType", blankToNull(contentType))
                .query((rs, rowNum) -> new ExtraMetricRow(rs.getString("platform_code"), rs.getString("extra_metrics"), localDateTime(rs, "captured_at")))
                .list()
                .forEach(row -> accumulators.computeIfAbsent(row.platformCode(), ignored -> new MetricAccumulator())
                        .add(readExtraMetric(row.extraMetrics(), metricKey), row.capturedAt()));
        return accumulators.entrySet().stream()
                .map(entry -> entry.getValue().toDto(metricKey, entry.getKey()))
                .toList();
    }

    public List<ContentSummaryDto> findContents(String platform, String contentType, String keyword,
                                                 LocalDate startDate, LocalDate endDate, int page, int pageSize) {
        int offset = Math.max(0, (page - 1) * pageSize);
        return jdbcClient.sql("""
                        select c.content_id, c.platform_code, c.external_content_id, c.content_type,
                               c.parent_content_id, c.title, a.display_name as account_name, c.category,
                               c.published_at, m.view_count, m.like_count, m.comment_count, m.share_count,
                               m.favorite_count, m.danmaku_count, m.coin_count, m.platform_heat_score,
                               m.normalized_heat_score, m.captured_at
                        from dim_content c
                        left join dim_account a on a.account_id = c.account_id
                        left join fact_content_metric_snapshot m on m.snapshot_id = (
                            select latest.snapshot_id from fact_content_metric_snapshot latest
                            where latest.content_id = c.content_id
                              and (:startDate is null or cast(latest.captured_at as date) >= :startDate)
                              and (:endDate is null or cast(latest.captured_at as date) <= :endDate)
                            order by latest.captured_at desc, latest.snapshot_id desc limit 1
                        )
                        where (:platform is null or c.platform_code = :platform)
                          and (:contentType is null or c.content_type = :contentType)
                          and (:keyword is null or lower(c.title) like lower(concat('%', :keyword, '%'))
                               or lower(c.external_content_id) like lower(concat('%', :keyword, '%'))
                               or lower(coalesce(a.display_name, '')) like lower(concat('%', :keyword, '%')))
                          and (:startDate is null or m.snapshot_id is not null)
                          and (:endDate is null or m.snapshot_id is not null)
                        order by coalesce(m.normalized_heat_score, -1) desc, coalesce(m.captured_at, c.published_at) desc, c.content_id desc
                        limit :pageSize offset :offset
                        """)
                .param("platform", blankToNull(platform))
                .param("contentType", blankToNull(contentType))
                .param("keyword", blankToNull(keyword))
                .param("startDate", startDate == null ? null : Date.valueOf(startDate))
                .param("endDate", endDate == null ? null : Date.valueOf(endDate))
                .param("pageSize", pageSize)
                .param("offset", offset)
                .query((rs, rowNum) -> toSummary(rs))
                .list();
    }

    public long countContents(String platform, String contentType, String keyword, LocalDate startDate, LocalDate endDate) {
        return jdbcClient.sql("""
                        select count(*) from dim_content c
                        left join dim_account a on a.account_id = c.account_id
                        left join fact_content_metric_snapshot m on m.snapshot_id = (
                            select latest.snapshot_id from fact_content_metric_snapshot latest
                            where latest.content_id = c.content_id
                              and (:startDate is null or cast(latest.captured_at as date) >= :startDate)
                              and (:endDate is null or cast(latest.captured_at as date) <= :endDate)
                            order by latest.captured_at desc, latest.snapshot_id desc limit 1
                        )
                        where (:platform is null or c.platform_code = :platform)
                          and (:contentType is null or c.content_type = :contentType)
                          and (:keyword is null or lower(c.title) like lower(concat('%', :keyword, '%'))
                               or lower(c.external_content_id) like lower(concat('%', :keyword, '%'))
                               or lower(coalesce(a.display_name, '')) like lower(concat('%', :keyword, '%')))
                          and (:startDate is null or m.snapshot_id is not null)
                          and (:endDate is null or m.snapshot_id is not null)
                        """)
                .param("platform", blankToNull(platform))
                .param("contentType", blankToNull(contentType))
                .param("keyword", blankToNull(keyword))
                .param("startDate", startDate == null ? null : Date.valueOf(startDate))
                .param("endDate", endDate == null ? null : Date.valueOf(endDate))
                .query(Long.class)
                .single();
    }

    public Optional<ContentSummaryDto> findContent(long contentId) {
        return jdbcClient.sql("""
                        select c.content_id, c.platform_code, c.external_content_id, c.content_type,
                               c.parent_content_id, c.title, a.display_name as account_name, c.category,
                               c.published_at, m.view_count, m.like_count, m.comment_count, m.share_count,
                               m.favorite_count, m.danmaku_count, m.coin_count, m.platform_heat_score,
                               m.normalized_heat_score, m.captured_at
                        from dim_content c
                        left join dim_account a on a.account_id = c.account_id
                        left join fact_content_metric_snapshot m on m.snapshot_id = (
                            select latest.snapshot_id from fact_content_metric_snapshot latest
                            where latest.content_id = c.content_id
                            order by latest.captured_at desc, latest.snapshot_id desc limit 1
                        )
                        where c.content_id = :contentId
                        """)
                .param("contentId", contentId)
                .query((rs, rowNum) -> toSummary(rs))
                .optional();
    }

    public List<ContentSummaryDto> findChildren(long parentContentId, int limit) {
        return jdbcClient.sql("""
                        select c.content_id, c.platform_code, c.external_content_id, c.content_type,
                               c.parent_content_id, c.title, a.display_name as account_name, c.category,
                               c.published_at, m.view_count, m.like_count, m.comment_count, m.share_count,
                               m.favorite_count, m.danmaku_count, m.coin_count, m.platform_heat_score,
                               m.normalized_heat_score, m.captured_at
                        from dim_content c
                        left join dim_account a on a.account_id = c.account_id
                        left join fact_content_metric_snapshot m on m.snapshot_id = (
                            select latest.snapshot_id from fact_content_metric_snapshot latest
                            where latest.content_id = c.content_id
                            order by latest.captured_at desc, latest.snapshot_id desc limit 1
                        )
                        where c.parent_content_id = :parentContentId
                        order by c.published_at, c.content_id
                        limit :limit
                        """)
                .param("parentContentId", parentContentId)
                .param("limit", limit)
                .query((rs, rowNum) -> toSummary(rs))
                .list();
    }

    public List<InteractionDto> findInteractions(long contentId, String interactionType, int limit) {
        return jdbcClient.sql("""
                        select i.interaction_id, i.content_id, i.platform_code, i.external_interaction_id,
                               i.interaction_type, i.user_name, i.text, i.like_count, i.video_time_seconds,
                               i.occurred_at, i.captured_at, a.sentiment_score, a.sentiment_label
                        from fact_interaction i
                        left join fact_text_analysis a on a.analysis_id = (
                            select max(latest.analysis_id) from fact_text_analysis latest
                            where latest.interaction_id = i.interaction_id
                        )
                        where i.content_id = :contentId
                          and (:interactionType is null or i.interaction_type = :interactionType)
                        order by coalesce(i.like_count, 0) desc, i.interaction_id desc
                        limit :limit
                        """)
                .param("contentId", contentId)
                .param("interactionType", blankToNull(interactionType))
                .param("limit", limit)
                .query((rs, rowNum) -> new InteractionDto(
                        rs.getLong("interaction_id"), rs.getLong("content_id"), rs.getString("platform_code"),
                        rs.getString("external_interaction_id"), rs.getString("interaction_type"),
                        rs.getString("user_name"), rs.getString("text"), nullableLong(rs, "like_count"),
                        nullableDouble(rs, "video_time_seconds"), localDateTime(rs, "occurred_at"),
                        localDateTime(rs, "captured_at"), nullableDouble(rs, "sentiment_score"),
                        rs.getString("sentiment_label")
                ))
                .list();
    }

    public SentimentSummaryDto findSentiment(long contentId) {
        return jdbcClient.sql("""
                        select count(a.analysis_id) as total_count,
                               sum(case when a.sentiment_label = 'positive' then 1 else 0 end) as positive_count,
                               sum(case when a.sentiment_label = 'neutral' then 1 else 0 end) as neutral_count,
                               sum(case when a.sentiment_label = 'negative' then 1 else 0 end) as negative_count,
                               avg(a.sentiment_score) as average_score
                        from fact_interaction i
                        left join fact_text_analysis a on a.analysis_id = (
                            select max(latest.analysis_id) from fact_text_analysis latest
                            where latest.interaction_id = i.interaction_id
                        )
                        where i.content_id = :contentId
                        """)
                .param("contentId", contentId)
                .query((rs, rowNum) -> {
                    long total = rs.getLong("total_count");
                    long positive = rs.getLong("positive_count");
                    long negative = rs.getLong("negative_count");
                    return new SentimentSummaryDto(
                            contentId, total, positive, rs.getLong("neutral_count"), negative,
                            nullableDouble(rs, "average_score"), total == 0 ? 0 : (double) positive / total,
                            total == 0 ? 0 : (double) negative / total
                    );
                })
                .single();
    }

    public List<TrendPointDto> findTrends(String platform, Date startDate, Date endDate) {
        return jdbcClient.sql("""
                        select cast(i.captured_at as date) as stat_date, i.interaction_type,
                               count(*) as interaction_count, avg(a.sentiment_score) as average_sentiment
                        from fact_interaction i
                        join dim_content c on c.content_id = i.content_id
                        left join fact_text_analysis a on a.analysis_id = (
                            select max(latest.analysis_id) from fact_text_analysis latest
                            where latest.interaction_id = i.interaction_id
                        )
                        where (:platform is null or c.platform_code = :platform)
                          and (:startDate is null or cast(i.captured_at as date) >= :startDate)
                          and (:endDate is null or cast(i.captured_at as date) <= :endDate)
                        group by cast(i.captured_at as date), i.interaction_type
                        order by stat_date, i.interaction_type
                        """)
                .param("platform", blankToNull(platform))
                .param("startDate", startDate)
                .param("endDate", endDate)
                .query((rs, rowNum) -> new TrendPointDto(
                        rs.getDate("stat_date").toLocalDate(), rs.getString("interaction_type"),
                        rs.getLong("interaction_count"), nullableDouble(rs, "average_sentiment")
                ))
                .list();
    }

    public CommentInsightSummaryDto findCommentInsightSummary(String platform, String interactionType,
                                                                Date startDate, Date endDate) {
        return jdbcClient.sql("""
                        select count(*) as interaction_count, count(a.analysis_id) as analyzed_count,
                               sum(case when a.sentiment_label = 'positive' then 1 else 0 end) as positive_count,
                               sum(case when a.sentiment_label = 'neutral' then 1 else 0 end) as neutral_count,
                               sum(case when a.sentiment_label = 'negative' then 1 else 0 end) as negative_count,
                               avg(a.sentiment_score) as average_sentiment
                        from fact_interaction i
                        join dim_content c on c.content_id = i.content_id
                        left join fact_text_analysis a on a.analysis_id = (
                            select max(latest.analysis_id) from fact_text_analysis latest
                            where latest.interaction_id = i.interaction_id
                        )
                        where (:platform is null or c.platform_code = :platform)
                          and ((:interactionType is null and i.interaction_type in ('comment', 'reply', 'review'))
                               or i.interaction_type = :interactionType)
                          and (:startDate is null or cast(coalesce(i.occurred_at, i.captured_at) as date) >= :startDate)
                          and (:endDate is null or cast(coalesce(i.occurred_at, i.captured_at) as date) <= :endDate)
                        """)
                .param("platform", blankToNull(platform))
                .param("interactionType", blankToNull(interactionType))
                .param("startDate", startDate)
                .param("endDate", endDate)
                .query((rs, rowNum) -> {
                    long analyzed = rs.getLong("analyzed_count");
                    long positive = rs.getLong("positive_count");
                    long neutral = rs.getLong("neutral_count");
                    long negative = rs.getLong("negative_count");
                    return new CommentInsightSummaryDto(
                            blankToNull(platform), blankToNull(interactionType), rs.getLong("interaction_count"),
                            analyzed, positive, neutral, negative, nullableDouble(rs, "average_sentiment"),
                            ratio(positive, analyzed), ratio(neutral, analyzed), ratio(negative, analyzed)
                    );
                })
                .single();
    }

    public List<InteractionTypeCountDto> findCommentInteractionTypes(String platform,
                                                                      Date startDate, Date endDate) {
        return jdbcClient.sql("""
                        select i.interaction_type, count(*) as interaction_count
                        from fact_interaction i
                        join dim_content c on c.content_id = i.content_id
                        where i.interaction_type in ('comment', 'reply', 'review')
                          and (:platform is null or c.platform_code = :platform)
                          and (:startDate is null or cast(coalesce(i.occurred_at, i.captured_at) as date) >= :startDate)
                          and (:endDate is null or cast(coalesce(i.occurred_at, i.captured_at) as date) <= :endDate)
                        group by i.interaction_type
                        order by interaction_count desc, i.interaction_type
                        """)
                .param("platform", blankToNull(platform))
                .param("startDate", startDate)
                .param("endDate", endDate)
                .query((rs, rowNum) -> new InteractionTypeCountDto(
                        rs.getString("interaction_type"), rs.getLong("interaction_count")
                ))
                .list();
    }

    public List<CommentTrendPointDto> findCommentInsightTrends(String platform, String interactionType,
                                                                Date startDate, Date endDate) {
        return jdbcClient.sql("""
                        select cast(coalesce(i.occurred_at, i.captured_at) as date) as stat_date,
                               count(*) as interaction_count, count(a.analysis_id) as analyzed_count,
                               sum(case when a.sentiment_label = 'negative' then 1 else 0 end) as negative_count,
                               avg(a.sentiment_score) as average_sentiment
                        from fact_interaction i
                        join dim_content c on c.content_id = i.content_id
                        left join fact_text_analysis a on a.analysis_id = (
                            select max(latest.analysis_id) from fact_text_analysis latest
                            where latest.interaction_id = i.interaction_id
                        )
                        where (:platform is null or c.platform_code = :platform)
                          and ((:interactionType is null and i.interaction_type in ('comment', 'reply', 'review'))
                               or i.interaction_type = :interactionType)
                          and (:startDate is null or cast(coalesce(i.occurred_at, i.captured_at) as date) >= :startDate)
                          and (:endDate is null or cast(coalesce(i.occurred_at, i.captured_at) as date) <= :endDate)
                        group by cast(coalesce(i.occurred_at, i.captured_at) as date)
                        order by stat_date
                        """)
                .param("platform", blankToNull(platform))
                .param("interactionType", blankToNull(interactionType))
                .param("startDate", startDate)
                .param("endDate", endDate)
                .query((rs, rowNum) -> {
                    long analyzed = rs.getLong("analyzed_count");
                    long negative = rs.getLong("negative_count");
                    return new CommentTrendPointDto(
                            rs.getDate("stat_date").toLocalDate(), rs.getLong("interaction_count"), analyzed,
                            nullableDouble(rs, "average_sentiment"), ratio(negative, analyzed)
                    );
                })
                .list();
    }

    public List<NegativeInteractionDto> findNegativeInteractions(String platform, String interactionType,
                                                                  Date startDate, Date endDate,
                                                                  int page, int pageSize) {
        int offset = Math.max(0, (page - 1) * pageSize);
        return jdbcClient.sql("""
                        select i.interaction_id, i.content_id, c.platform_code, c.external_content_id,
                               c.title as content_title, i.interaction_type, i.user_name, i.text, i.like_count,
                               i.occurred_at, i.captured_at, a.sentiment_score
                        from fact_interaction i
                        join dim_content c on c.content_id = i.content_id
                        join fact_text_analysis a on a.analysis_id = (
                            select max(latest.analysis_id) from fact_text_analysis latest
                            where latest.interaction_id = i.interaction_id
                        )
                        where a.sentiment_label = 'negative'
                          and not exists (
                              select 1
                              from ops_task t
                              join ops_task_status s on s.task_id = t.task_id
                              where t.task_id = concat('manual-negative-interaction-', i.interaction_id)
                                and t.source = 'manual'
                                and s.status in ('done', 'ignored')
                          )
                          and (:platform is null or c.platform_code = :platform)
                          and ((:interactionType is null and i.interaction_type in ('comment', 'reply', 'review'))
                               or i.interaction_type = :interactionType)
                          and (:startDate is null or cast(coalesce(i.occurred_at, i.captured_at) as date) >= :startDate)
                          and (:endDate is null or cast(coalesce(i.occurred_at, i.captured_at) as date) <= :endDate)
                        order by coalesce(i.like_count, 0) desc, coalesce(i.occurred_at, i.captured_at) desc,
                                 i.interaction_id desc
                        limit :pageSize offset :offset
                        """)
                .param("platform", blankToNull(platform))
                .param("interactionType", blankToNull(interactionType))
                .param("startDate", startDate)
                .param("endDate", endDate)
                .param("pageSize", pageSize)
                .param("offset", offset)
                .query((rs, rowNum) -> new NegativeInteractionDto(
                        rs.getLong("interaction_id"), rs.getLong("content_id"), rs.getString("platform_code"),
                        rs.getString("external_content_id"), rs.getString("content_title"),
                        rs.getString("interaction_type"), rs.getString("user_name"), rs.getString("text"),
                        nullableLong(rs, "like_count"), localDateTime(rs, "occurred_at"),
                        localDateTime(rs, "captured_at"), nullableDouble(rs, "sentiment_score")
                ))
                .list();
    }

    public Optional<NegativeInteractionDto> findNegativeInteraction(long interactionId) {
        return jdbcClient.sql("""
                        select i.interaction_id, i.content_id, c.platform_code, c.external_content_id,
                               c.title as content_title, i.interaction_type, i.user_name, i.text, i.like_count,
                               i.occurred_at, i.captured_at, a.sentiment_score
                        from fact_interaction i
                        join dim_content c on c.content_id = i.content_id
                        join fact_text_analysis a on a.analysis_id = (
                            select max(latest.analysis_id) from fact_text_analysis latest
                            where latest.interaction_id = i.interaction_id
                        )
                        where i.interaction_id = :interactionId and a.sentiment_label = 'negative'
                        """)
                .param("interactionId", interactionId)
                .query((rs, rowNum) -> new NegativeInteractionDto(
                        rs.getLong("interaction_id"), rs.getLong("content_id"), rs.getString("platform_code"),
                        rs.getString("external_content_id"), rs.getString("content_title"),
                        rs.getString("interaction_type"), rs.getString("user_name"), rs.getString("text"),
                        nullableLong(rs, "like_count"), localDateTime(rs, "occurred_at"),
                        localDateTime(rs, "captured_at"), nullableDouble(rs, "sentiment_score")
                ))
                .optional();
    }

    public long countNegativeInteractions(String platform, String interactionType, Date startDate, Date endDate) {
        return jdbcClient.sql("""
                        select count(*)
                        from fact_interaction i
                        join dim_content c on c.content_id = i.content_id
                        join fact_text_analysis a on a.analysis_id = (
                            select max(latest.analysis_id) from fact_text_analysis latest
                            where latest.interaction_id = i.interaction_id
                        )
                        where a.sentiment_label = 'negative'
                          and not exists (
                              select 1
                              from ops_task t
                              join ops_task_status s on s.task_id = t.task_id
                              where t.task_id = concat('manual-negative-interaction-', i.interaction_id)
                                and t.source = 'manual'
                                and s.status in ('done', 'ignored')
                          )
                          and (:platform is null or c.platform_code = :platform)
                          and ((:interactionType is null and i.interaction_type in ('comment', 'reply', 'review'))
                               or i.interaction_type = :interactionType)
                          and (:startDate is null or cast(coalesce(i.occurred_at, i.captured_at) as date) >= :startDate)
                          and (:endDate is null or cast(coalesce(i.occurred_at, i.captured_at) as date) <= :endDate)
                        """)
                .param("platform", blankToNull(platform))
                .param("interactionType", blankToNull(interactionType))
                .param("startDate", startDate)
                .param("endDate", endDate)
                .query(Long.class)
                .single();
    }

    public List<String> findKeywordTexts(String platform, Long contentId) {
        return jdbcClient.sql("""
                        select a.keywords
                        from fact_text_analysis a
                        join fact_interaction i on i.interaction_id = a.interaction_id
                        join dim_content c on c.content_id = i.content_id
                        where a.keywords is not null and a.keywords <> ''
                          and (:platform is null or c.platform_code = :platform)
                          and (:contentId is null or c.content_id = :contentId)
                        """)
                .param("platform", blankToNull(platform))
                .param("contentId", contentId)
                .query(String.class)
                .list();
    }

    public List<CommentTopicDto> findCommentTopics(String platform, String interactionType, Date startDate, Date endDate, int limit) {
        Map<String, long[]> counts = new LinkedHashMap<>();
        jdbcClient.sql("""
                        select a.keywords, a.sentiment_label
                        from fact_text_analysis a
                        join fact_interaction i on i.interaction_id = a.interaction_id
                        join dim_content c on c.content_id = i.content_id
                        where a.analysis_id = (
                            select max(latest.analysis_id) from fact_text_analysis latest
                            where latest.interaction_id = i.interaction_id
                        )
                          and a.keywords is not null and a.keywords <> ''
                          and (:platform is null or c.platform_code = :platform)
                          and (:interactionType is null or i.interaction_type = :interactionType)
                          and (:startDate is null or coalesce(i.occurred_at, i.captured_at) >= :startDate)
                          and (:endDate is null or coalesce(i.occurred_at, i.captured_at) < date_add(:endDate, interval 1 day))
                        """)
                .param("platform", blankToNull(platform))
                .param("interactionType", blankToNull(interactionType))
                .param("startDate", startDate)
                .param("endDate", endDate)
                .query((rs, rowNum) -> new TopicKeywords(rs.getString("keywords"), rs.getString("sentiment_label")))
                .list()
                .forEach(row -> splitKeywords(row.keywords()).forEach(topic -> {
                    long[] topicCounts = counts.computeIfAbsent(topic, ignored -> new long[2]);
                    topicCounts[0]++;
                    if ("negative".equals(row.sentimentLabel())) topicCounts[1]++;
                }));
        return counts.entrySet().stream()
                .sorted((left, right) -> Long.compare(right.getValue()[0], left.getValue()[0]))
                .limit(limit)
                .map(entry -> new CommentTopicDto(entry.getKey(), entry.getValue()[0], entry.getValue()[1]))
                .toList();
    }

    public List<AccountPerformanceDto> findAccountPerformance(String platform, int limit) {
        return jdbcClient.sql("""
                        select a.account_id, a.platform_code, a.external_account_id, a.display_name, a.account_type,
                               count(distinct c.content_id) as content_count, sum(m.view_count) as total_view_count,
                               sum(m.like_count) as total_like_count, avg(m.normalized_heat_score) as average_normalized_heat
                        from dim_account a
                        left join dim_content c on c.account_id = a.account_id
                        left join fact_content_metric_snapshot m on m.snapshot_id = (
                            select latest.snapshot_id from fact_content_metric_snapshot latest
                            where latest.content_id = c.content_id
                            order by latest.captured_at desc, latest.snapshot_id desc limit 1
                        )
                        where (:platform is null or a.platform_code = :platform)
                        group by a.account_id, a.platform_code, a.external_account_id, a.display_name, a.account_type
                        order by coalesce(avg(m.normalized_heat_score), -1) desc, sum(m.view_count) desc
                        limit :limit
                        """)
                .param("platform", blankToNull(platform))
                .param("limit", limit)
                .query((rs, rowNum) -> new AccountPerformanceDto(
                        rs.getLong("account_id"), rs.getString("platform_code"),
                        rs.getString("external_account_id"), rs.getString("display_name"),
                        rs.getString("account_type"), rs.getLong("content_count"),
                        nullableLong(rs, "total_view_count"), nullableLong(rs, "total_like_count"),
                        nullableDouble(rs, "average_normalized_heat")
                ))
                .list();
    }

    public List<TimelinePointDto> findTimeline(long contentId, String interactionType) {
        return jdbcClient.sql("""
                        select floor(coalesce(i.video_time_seconds, 0) / 30) * 30 as time_bucket,
                               i.interaction_type, count(*) as interaction_count,
                               avg(a.sentiment_score) as average_sentiment
                        from fact_interaction i
                        left join fact_text_analysis a on a.analysis_id = (
                            select max(latest.analysis_id) from fact_text_analysis latest
                            where latest.interaction_id = i.interaction_id
                        )
                        where i.content_id = :contentId and i.interaction_type = :interactionType
                        group by floor(coalesce(i.video_time_seconds, 0) / 30) * 30, i.interaction_type
                        order by time_bucket
                        """)
                .param("contentId", contentId)
                .param("interactionType", interactionType)
                .query((rs, rowNum) -> new TimelinePointDto(
                        contentId, rs.getString("interaction_type"), rs.getInt("time_bucket"),
                        rs.getLong("interaction_count"), nullableDouble(rs, "average_sentiment")
                ))
                .list();
    }

    private ContentSummaryDto toSummary(ResultSet rs) throws SQLException {
        return new ContentSummaryDto(
                rs.getLong("content_id"),
                rs.getString("platform_code"),
                rs.getString("external_content_id"),
                rs.getString("content_type"),
                nullableLong(rs, "parent_content_id"),
                rs.getString("title"),
                rs.getString("account_name"),
                rs.getString("category"),
                localDateTime(rs, "published_at"),
                nullableLong(rs, "view_count"),
                nullableLong(rs, "like_count"),
                nullableLong(rs, "comment_count"),
                nullableLong(rs, "share_count"),
                nullableLong(rs, "favorite_count"),
                nullableLong(rs, "danmaku_count"),
                nullableLong(rs, "coin_count"),
                nullableDouble(rs, "platform_heat_score"),
                nullableDouble(rs, "normalized_heat_score"),
                localDateTime(rs, "captured_at")
        );
    }

    private static MetricDefinitionDto toMetricDefinition(ResultSet rs) throws SQLException {
        return new MetricDefinitionDto(
                rs.getString("metric_key"), rs.getString("display_name"), rs.getString("unit"),
                rs.getString("scope"), rs.getString("definition"), rs.getBoolean("comparable")
        );
    }

    private static String metricExpression(String metricKey) {
        return switch (metricKey) {
            case "view_count", "like_count", "comment_count", "share_count", "favorite_count",
                    "danmaku_count", "coin_count", "completion_rate", "rating", "platform_heat_score",
                    "normalized_heat_score" -> "m." + metricKey;
            case "interaction_rate" -> "(coalesce(m.like_count, 0) + coalesce(m.comment_count, 0)"
                    + " + coalesce(m.share_count, 0) + coalesce(m.favorite_count, 0)"
                    + " + coalesce(m.coin_count, 0) + coalesce(m.danmaku_count, 0))"
                    + " * 1.0 / nullif(m.view_count, 0)";
            default -> null;
        };
    }

    private Double readExtraMetric(String json, String metricKey) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            Object value = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {}).get(metricKey);
            if (value instanceof Number number) {
                return finiteNumber(number.doubleValue());
            }
            if (value instanceof String text) {
                return finiteNumber(Double.parseDouble(text));
            }
        } catch (Exception ignored) {
            // Optional connector data is unavailable when malformed.
        }
        return null;
    }

    private static List<String> splitKeywords(String keywords) {
        if (keywords == null || keywords.isBlank()) return List.of();
        List<String> result = new ArrayList<>();
        for (String candidate : keywords.split("[，,;；\\s]+")) {
            String topic = normalizeTopic(candidate);
            if (topic != null && !result.contains(topic)) result.add(topic);
        }
        return result;
    }

    private static String normalizeTopic(String candidate) {
        String topic = candidate == null ? "" : candidate.trim().toLowerCase();
        if (topic.length() < 2 || topic.length() > 32 || topic.matches("[0-9.]+") || TOPIC_STOP_WORDS.contains(topic)) {
            return null;
        }
        return TOPIC_SYNONYMS.getOrDefault(topic, topic);
    }

    private static Double finiteNumber(double value) {
        return Double.isFinite(value) ? value : null;
    }

    private record ExtraMetricRow(String platformCode, String extraMetrics, LocalDateTime capturedAt) {
    }

    private record TopicKeywords(String keywords, String sentimentLabel) {
    }

    private static final class MetricAccumulator {
        private long contentCount;
        private long availableCount;
        private double sum;
        private Double min;
        private Double max;
        private LocalDateTime latestCapturedAt;

        private void add(Double value, LocalDateTime capturedAt) {
            contentCount++;
            if (capturedAt != null && (latestCapturedAt == null || capturedAt.isAfter(latestCapturedAt))) {
                latestCapturedAt = capturedAt;
            }
            if (value == null) {
                return;
            }
            availableCount++;
            sum += value;
            min = min == null ? value : Math.min(min, value);
            max = max == null ? value : Math.max(max, value);
        }

        private MetricComparisonDto toDto(String metricKey, String platformCode) {
            return new MetricComparisonDto(metricKey, platformCode, contentCount, availableCount,
                    availableCount == 0 ? null : sum / availableCount, min, max, latestCapturedAt);
        }
    }

    private Map<String, Boolean> readCapabilities(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception exception) {
            throw new IllegalStateException("平台能力配置不是合法 JSON", exception);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private static Double nullableDouble(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }

    private static double ratio(long count, long total) {
        return total == 0 ? 0 : (double) count / total;
    }

    private static LocalDateTime localDateTime(ResultSet rs, String column) throws SQLException {
        var timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
