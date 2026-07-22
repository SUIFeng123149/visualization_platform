package com.bililens.analytics.platform.repository;

import com.bililens.analytics.platform.dto.AnomalyRuleDto;
import com.bililens.analytics.platform.dto.AnomalyRuleUpdateRequest;
import com.bililens.analytics.platform.dto.DataSourceStatusDto;
import com.bililens.analytics.platform.dto.ReportCreateRequest;
import com.bililens.analytics.platform.dto.ReportHistoryDto;
import com.bililens.analytics.platform.dto.PlatformConfigDto;
import com.bililens.analytics.platform.dto.PlatformConfigRequest;
import com.bililens.analytics.platform.dto.MetricConfigDto;
import com.bililens.analytics.platform.dto.MetricConfigRequest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class PlatformRepository {

    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;

    public PlatformRepository(JdbcClient jdbcClient, ObjectMapper objectMapper) {
        this.jdbcClient = jdbcClient;
        this.objectMapper = objectMapper;
    }

    public List<PlatformConfigDto> findPlatformConfigs() {
        return jdbcClient.sql("select platform_code, display_name, connector_name, capabilities_json, enabled from dim_platform order by platform_code")
                .query((rs, rowNum) -> new PlatformConfigDto(rs.getString("platform_code"), rs.getString("display_name"),
                        rs.getString("connector_name"), capabilities(rs.getString("capabilities_json")), rs.getBoolean("enabled")))
                .list();
    }

    public PlatformConfigDto upsertPlatformConfig(PlatformConfigRequest request) {
        String json = writeCapabilities(request.capabilities());
        int updated = jdbcClient.sql("""
                        update dim_platform set display_name=:displayName, connector_name=:connectorName,
                        capabilities_json=:capabilities, enabled=:enabled where platform_code=:platformCode
                        """)
                .param("platformCode", request.platformCode()).param("displayName", request.displayName())
                .param("connectorName", request.connectorName()).param("capabilities", json).param("enabled", request.enabled()).update();
        if (updated == 0) {
            jdbcClient.sql("""
                            insert into dim_platform (platform_code, display_name, connector_name, capabilities_json, enabled)
                            values (:platformCode, :displayName, :connectorName, :capabilities, :enabled)
                            """)
                    .param("platformCode", request.platformCode()).param("displayName", request.displayName())
                    .param("connectorName", request.connectorName()).param("capabilities", json).param("enabled", request.enabled()).update();
        }
        return findPlatformConfigs().stream().filter(item -> item.platformCode().equals(request.platformCode())).findFirst().orElseThrow();
    }

    public void deletePlatformConfig(String platformCode) {
        Long accounts = jdbcClient.sql("select count(*) from dim_account where platform_code=:platformCode")
                .param("platformCode", platformCode).query(Long.class).single();
        Long contents = jdbcClient.sql("select count(*) from dim_content where platform_code=:platformCode")
                .param("platformCode", platformCode).query(Long.class).single();
        if ((accounts != null && accounts > 0) || (contents != null && contents > 0)) {
            throw new IllegalArgumentException("该平台仍有关联账号或内容，请先停用或迁移数据");
        }
        int deleted = jdbcClient.sql("delete from dim_platform where platform_code=:platformCode")
                .param("platformCode", platformCode).update();
        if (deleted == 0) throw new IllegalArgumentException("平台不存在: " + platformCode);
    }

    public List<MetricConfigDto> findMetricConfigs() {
        return jdbcClient.sql("select metric_key, display_name, unit, scope, definition, comparable from metric_dictionary order by display_name")
                .query((rs, rowNum) -> new MetricConfigDto(rs.getString("metric_key"), rs.getString("display_name"),
                        rs.getString("unit"), rs.getString("scope"), rs.getString("definition"), rs.getBoolean("comparable"))).list();
    }

    public MetricConfigDto updateMetricConfig(String metricKey, MetricConfigRequest request) {
        int updated = jdbcClient.sql("""
                        update metric_dictionary set display_name=:displayName, unit=:unit, scope=:scope,
                        definition=:definition, comparable=:comparable where metric_key=:metricKey
                        """)
                .param("metricKey", metricKey).param("displayName", request.displayName()).param("unit", request.unit())
                .param("scope", request.scope()).param("definition", request.definition()).param("comparable", request.comparable()).update();
        if (updated == 0) throw new IllegalArgumentException("指标不存在: " + metricKey);
        return findMetricConfigs().stream().filter(item -> item.metricKey().equals(metricKey)).findFirst().orElseThrow();
    }

    public List<DataSourceStatusDto> findDataSourceStatuses() {
        List<DataSourceStatusDto> statuses = new ArrayList<>(List.of(
                tableStatus("dim_platform", "平台目录（v2）", "DIM", null),
                tableStatus("dim_account", "发布账号（v2）", "DIM", null),
                tableStatus("dim_content", "统一内容（v2）", "DIM", null),
                tableStatus("fact_content_metric_snapshot", "内容指标快照（v2）", "FACT", "captured_at"),
                tableStatus("fact_interaction", "统一互动明细（v2）", "FACT", "captured_at"),
                tableStatus("fact_text_analysis", "文本分析结果（v2）", "FACT", "created_at"),
                tableStatus("metric_dictionary", "指标字典（v2）", "CONFIG", "created_at"),
                tableStatus("collector_task", "采集任务", "OPS", "updated_at"),
                tableStatus("ops_task", "运营任务", "OPS", "updated_at")
        ));
        statuses.addAll(findPlatformIngestionStatuses());
        return statuses;
    }

    private List<DataSourceStatusDto> findPlatformIngestionStatuses() {
        try {
            return jdbcClient.sql("""
                            select p.platform_code, p.display_name, p.connector_name,
                                   count(distinct c.content_id) as content_count,
                                   count(s.snapshot_id) as snapshot_count,
                                   max(s.captured_at) as latest_at
                            from dim_platform p
                            left join dim_content c on c.platform_code = p.platform_code
                            left join fact_content_metric_snapshot s on s.content_id = c.content_id
                            where p.enabled = 1
                            group by p.platform_code, p.display_name, p.connector_name
                            order by p.platform_code
                            """)
                    .query((rs, rowNum) -> {
                        long contents = rs.getLong("content_count");
                        long snapshots = rs.getLong("snapshot_count");
                        LocalDateTime latestAt = toLocalDateTime(rs.getTimestamp("latest_at"));
                        String status = snapshots == 0 ? "empty"
                                : latestAt != null && latestAt.isBefore(LocalDateTime.now().minusDays(7)) ? "stale"
                                : "healthy";
                        String message = "connector=" + rs.getString("connector_name")
                                + ", contents=" + contents + ", snapshots=" + snapshots;
                        return new DataSourceStatusDto(
                                "platform:" + rs.getString("platform_code"),
                                rs.getString("display_name") + " ingestion",
                                "PLATFORM", snapshots, latestAt, status, message
                        );
                    })
                    .list();
        } catch (DataAccessException error) {
            return List.of();
        }
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

    private java.util.Map<String, Boolean> capabilities(String json) {
        try {
            return json == null || json.isBlank() ? java.util.Map.of()
                    : objectMapper.readValue(json, new TypeReference<java.util.Map<String, Boolean>>() {});
        } catch (Exception error) {
            throw new IllegalArgumentException("平台能力配置不是合法 JSON", error);
        }
    }

    private String writeCapabilities(java.util.Map<String, Boolean> capabilities) {
        try {
            return objectMapper.writeValueAsString(capabilities == null ? java.util.Map.of() : capabilities);
        } catch (Exception error) {
            throw new IllegalArgumentException("平台能力配置无法保存", error);
        }
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
