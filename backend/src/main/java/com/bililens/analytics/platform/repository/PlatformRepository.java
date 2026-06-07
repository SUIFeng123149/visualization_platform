package com.bililens.analytics.platform.repository;

import com.bililens.analytics.platform.dto.AnomalyRuleDto;
import com.bililens.analytics.platform.dto.AnomalyRuleUpdateRequest;
import com.bililens.analytics.platform.dto.DataSourceStatusDto;
import com.bililens.analytics.platform.dto.ReportCreateRequest;
import com.bililens.analytics.platform.dto.ReportHistoryDto;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class PlatformRepository {

    private final JdbcClient jdbcClient;

    public PlatformRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<DataSourceStatusDto> findDataSourceStatuses() {
        return List.of(
                tableStatus("dwd_comment_clean", "清洗评论明细", "DWD", "created_at"),
                tableStatus("dwd_danmaku_clean", "清洗弹幕明细", "DWD", "created_at"),
                tableStatus("dws_text_analysis_detail", "文本分析明细", "DWS", "created_at"),
                tableStatus("ads_video_heat_rank", "视频热度排行", "ADS", null),
                tableStatus("ads_video_sentiment", "视频情感统计", "ADS", null),
                tableStatus("ads_sentiment_by_date", "情感趋势汇总", "ADS", "stat_date"),
                tableStatus("ads_danmaku_timeline", "弹幕时间轴", "ADS", null),
                tableStatus("ads_keyword_top", "关键词排行", "ADS", null),
                tableStatus("ads_up_performance", "UP主表现汇总", "ADS", null),
                tableStatus("ops_task", "运营任务", "OPS", "updated_at")
        );
    }

    public List<ReportHistoryDto> findReportHistory() {
        try {
            return jdbcClient.sql("""
                            select id, report_name, report_type, status, row_count, file_name, remark, created_at
                            from ops_report_history
                            order by created_at desc, id desc
                            limit 50
                            """)
                    .query((rs, rowNum) -> new ReportHistoryDto(
                            rs.getLong("id"),
                            rs.getString("report_name"),
                            rs.getString("report_type"),
                            rs.getString("status"),
                            rs.getLong("row_count"),
                            rs.getString("file_name"),
                            rs.getString("remark"),
                            toLocalDateTime(rs.getTimestamp("created_at"))
                    ))
                    .list();
        } catch (DataAccessException ignored) {
            return List.of();
        }
    }

    public ReportHistoryDto createReportHistory(ReportCreateRequest request) {
        try {
            jdbcClient.sql("""
                            insert into ops_report_history
                            (report_name, report_type, status, row_count, file_name, remark, created_at)
                            values (:reportName, :reportType, 'success', :rowCount, :fileName, :remark, current_timestamp)
                            """)
                    .param("reportName", defaultText(request.reportName(), "未命名报表"))
                    .param("reportType", defaultText(request.reportType(), "custom"))
                    .param("rowCount", request.rowCount() == null ? 0L : request.rowCount())
                    .param("fileName", request.fileName())
                    .param("remark", request.remark())
                    .update();

            return jdbcClient.sql("""
                            select id, report_name, report_type, status, row_count, file_name, remark, created_at
                            from ops_report_history
                            order by id desc
                            limit 1
                            """)
                    .query((rs, rowNum) -> new ReportHistoryDto(
                            rs.getLong("id"),
                            rs.getString("report_name"),
                            rs.getString("report_type"),
                            rs.getString("status"),
                            rs.getLong("row_count"),
                            rs.getString("file_name"),
                            rs.getString("remark"),
                            toLocalDateTime(rs.getTimestamp("created_at"))
                    ))
                    .single();
        } catch (DataAccessException ignored) {
            return new ReportHistoryDto(
                    null,
                    defaultText(request.reportName(), "未命名报表"),
                    defaultText(request.reportType(), "custom"),
                    "memory",
                    request.rowCount() == null ? 0L : request.rowCount(),
                    request.fileName(),
                    "ops_report_history 表不存在，当前记录未持久化",
                    LocalDateTime.now()
            );
        }
    }

    public List<AnomalyRuleDto> findAnomalyRules() {
        try {
            List<AnomalyRuleDto> rules = jdbcClient.sql("""
                            select id, rule_key, name, metric, operator, threshold_value, level, enabled, description, updated_at
                            from ops_anomaly_rule
                            order by id
                            """)
                    .query((rs, rowNum) -> new AnomalyRuleDto(
                            rs.getLong("id"),
                            rs.getString("rule_key"),
                            rs.getString("name"),
                            rs.getString("metric"),
                            rs.getString("operator"),
                            rs.getDouble("threshold_value"),
                            rs.getString("level"),
                            rs.getBoolean("enabled"),
                            rs.getString("description"),
                            toLocalDateTime(rs.getTimestamp("updated_at"))
                    ))
                    .list();
            return rules.isEmpty() ? defaultRules() : rules;
        } catch (DataAccessException ignored) {
            return defaultRules();
        }
    }

    public AnomalyRuleDto upsertAnomalyRule(AnomalyRuleUpdateRequest request) {
        try {
            int updated = jdbcClient.sql("""
                            update ops_anomaly_rule
                            set name = :name,
                                metric = :metric,
                                operator = :operator,
                                threshold_value = :threshold,
                                level = :level,
                                enabled = :enabled,
                                description = :description,
                                updated_at = current_timestamp
                            where rule_key = :ruleKey
                            """)
                    .param("ruleKey", request.ruleKey())
                    .param("name", request.name())
                    .param("metric", request.metric())
                    .param("operator", request.operator())
                    .param("threshold", request.threshold())
                    .param("level", request.level())
                    .param("enabled", request.enabled())
                    .param("description", request.description())
                    .update();

            if (updated == 0) {
                jdbcClient.sql("""
                                insert into ops_anomaly_rule
                                (rule_key, name, metric, operator, threshold_value, level, enabled, description, created_at, updated_at)
                                values (:ruleKey, :name, :metric, :operator, :threshold, :level, :enabled, :description, current_timestamp, current_timestamp)
                                """)
                        .param("ruleKey", request.ruleKey())
                        .param("name", request.name())
                        .param("metric", request.metric())
                        .param("operator", request.operator())
                        .param("threshold", request.threshold())
                        .param("level", request.level())
                        .param("enabled", request.enabled())
                        .param("description", request.description())
                        .update();
            }

            return findAnomalyRules().stream()
                    .filter(rule -> rule.ruleKey().equals(request.ruleKey()))
                    .findFirst()
                    .orElseGet(() -> fallbackRule(request));
        } catch (DataAccessException ignored) {
            return fallbackRule(request);
        }
    }

    private DataSourceStatusDto tableStatus(String tableName, String displayName, String layer, String timeColumn) {
        try {
            String latestSelect = timeColumn == null ? "null as latest_at" : "max(" + timeColumn + ") as latest_at";
            return jdbcClient.sql("select count(*) as row_count, " + latestSelect + " from " + tableName)
                    .query((rs, rowNum) -> {
                        long rowCount = rs.getLong("row_count");
                        LocalDateTime latestAt = toLocalDateTime(rs.getTimestamp("latest_at"));
                        String status = rowCount > 0 ? "healthy" : "empty";
                        String message = rowCount > 0 ? "数据可用" : "暂无数据，请检查同步任务";
                        return new DataSourceStatusDto(tableName, displayName, layer, rowCount, latestAt, status, message);
                    })
                    .single();
        } catch (DataAccessException error) {
            return new DataSourceStatusDto(tableName, displayName, layer, 0, null, "missing", "表不存在或无法访问");
        }
    }

    private static List<AnomalyRuleDto> defaultRules() {
        LocalDateTime now = LocalDateTime.now();
        return List.of(
                new AnomalyRuleDto(null, "heat_top", "热度异常高", "heatScore", ">=", 8000, "warning", true, "热度分超过阈值时提示优先复盘。", now),
                new AnomalyRuleDto(null, "negative_risk", "负向情绪预警", "negativeRatio", ">=", 0.2, "danger", true, "负向占比超过阈值时提示舆情风险。", now),
                new AnomalyRuleDto(null, "interaction_high", "互动效率突出", "interactionRate", ">=", 0.08, "success", true, "互动率超过阈值时提示增长样本。", now),
                new AnomalyRuleDto(null, "danmaku_hotspot", "弹幕峰值片段", "danmakuCount", ">=", 100, "primary", true, "弹幕峰值超过阈值时提示切片机会。", now)
        );
    }

    private static AnomalyRuleDto fallbackRule(AnomalyRuleUpdateRequest request) {
        return new AnomalyRuleDto(
                null,
                request.ruleKey(),
                request.name(),
                request.metric(),
                request.operator(),
                request.threshold(),
                request.level(),
                request.enabled(),
                "ops_anomaly_rule 表不存在，当前规则未持久化",
                LocalDateTime.now()
        );
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
