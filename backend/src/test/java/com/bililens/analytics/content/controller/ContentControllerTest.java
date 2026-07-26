package com.bililens.analytics.content.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void platformsExposeCapabilities() throws Exception {
        mockMvc.perform(get("/api/v2/platforms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(4)))
                .andExpect(jsonPath("$.data[0].connectorName").value("bilibili-export-v1"))
                .andExpect(jsonPath("$.data[0].capabilities.comments").value(true));
    }

    @Test
    void platformPreflightRejectsUnknownOrDisabledPlatforms() throws Exception {
        mockMvc.perform(post("/api/platform/platforms/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"platformCodes\":[\"bilibili\",\"kuaishou\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(false))
                .andExpect(jsonPath("$.data.configuredPlatformCodes", hasItem("bilibili")))
                .andExpect(jsonPath("$.data.missingPlatformCodes", hasItem("kuaishou")));
    }

    @Test
    void legacyAnalysisEndpointsAreDisabledByDefault() throws Exception {
        mockMvc.perform(get("/api/analysis/videos/heat-rank"))
                .andExpect(status().isNotFound());
    }

    @Test
    void dataSourceMonitoringUsesV2TablesAndPlatformIngestionCoverage() throws Exception {
        mockMvc.perform(get("/api/platform/data-sources/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.tableName == 'fact_content_metric_snapshot')]").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.tableName == 'platform:bilibili')]").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.tableName == 'ads_video_heat_rank')]").isEmpty());
    }

    @Test
    void contentsSupportPlatformFilterAndNullableMetrics() throws Exception {
        mockMvc.perform(get("/api/v2/contents").param("platform", "douyin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].externalContentId").value("DY001"))
                .andExpect(jsonPath("$.data[0].danmakuCount").doesNotExist());
    }

    @Test
    void contentsAcceptTheFullPageDisplayLimit() throws Exception {
        mockMvc.perform(get("/api/v2/contents").param("limit", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void metricComparisonStaysOutsideTheContentReviewContract() throws Exception {
        mockMvc.perform(get("/api/v2/analytics/metric-definitions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(4)))
                .andExpect(jsonPath("$.data[0].metricKey").value("interaction_rate"));

        mockMvc.perform(get("/api/v2/analytics/metric-comparison").param("metricKey", "normalized_heat_score"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(4)))
                .andExpect(jsonPath("$.data[0].metricKey").value("normalized_heat_score"));
    }

    @Test
    void metricComparisonSupportsRegisteredConnectorSpecificMetrics() throws Exception {
        mockMvc.perform(get("/api/v2/analytics/metric-comparison").param("metricKey", "completion_events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(4)))
                .andExpect(jsonPath("$.data[0].metricKey").value("completion_events"))
                .andExpect(jsonPath("$.data[0].availableCount").value(1))
                .andExpect(jsonPath("$.data[0].averageValue").value(510.0));
    }

    @Test
    void pagedContentsSupportKeywordAndDateRange() throws Exception {
        mockMvc.perform(get("/api/v2/contents/page")
                        .param("keyword", "短视频")
                        .param("startDate", "2026-05-26")
                        .param("endDate", "2026-05-26")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.page").value(1));
    }

    @Test
    void contentDetailUsesInternalContentId() throws Exception {
        mockMvc.perform(get("/api/v2/contents/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.platformCode").value("bilibili"))
                .andExpect(jsonPath("$.data.externalContentId").value("BV003"));
    }

    @Test
    void interactionsCanBeFilteredByPlatformNeutralType() throws Exception {
        mockMvc.perform(get("/api/v2/contents/1/interactions").param("type", "danmaku"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].interactionType").value("danmaku"))
                .andExpect(jsonPath("$.data[0].sentimentLabel").exists());
    }

    @Test
    void sentimentIsAggregatedFromMockTextAnalysis() throws Exception {
        mockMvc.perform(get("/api/v2/contents/1/sentiment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(4))
                .andExpect(jsonPath("$.data.positiveCount").value(2))
                .andExpect(jsonPath("$.data.negativeCount").value(2));
    }

    @Test
    void timelineUsesGenericInteractionType() throws Exception {
        mockMvc.perform(get("/api/v2/contents/1/timeline").param("type", "danmaku"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].timeBucket").value(60))
                .andExpect(jsonPath("$.data[0].interactionCount").value(2));
    }

    @Test
    void trendsAndKeywordsCanBeFilteredByPlatform() throws Exception {
        mockMvc.perform(get("/api/v2/analytics/trends").param("platform", "douyin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].interactionType").value("comment"));

        mockMvc.perform(get("/api/v2/analytics/keywords").param("platform", "douyin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].word").value("信息"));
    }

    @Test
    void accountPerformanceAndSeriesChildrenArePlatformNeutral() throws Exception {
        mockMvc.perform(get("/api/v2/accounts/performance").param("platform", "iqiyi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].displayName").value("星河影视"))
                .andExpect(jsonPath("$.data[0].contentCount").value(2));

        mockMvc.perform(get("/api/v2/contents/3/children"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].contentType").value("episode"));
    }

    @Test
    void commentInsightsAggregateAcrossPlatformNeutralInteractionTypes() throws Exception {
        mockMvc.perform(get("/api/v2/analytics/comments/summary").param("platform", "douyin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.interactionCount").value(3))
                .andExpect(jsonPath("$.data.analyzedCount").value(3))
                .andExpect(jsonPath("$.data.positiveCount").value(1))
                .andExpect(jsonPath("$.data.neutralCount").value(1))
                .andExpect(jsonPath("$.data.negativeCount").value(1));

        mockMvc.perform(get("/api/v2/analytics/comments/summary")
                        .param("platform", "youku")
                        .param("type", "review"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.interactionCount").value(1))
                .andExpect(jsonPath("$.data.positiveRatio").value(1.0));
    }

    @Test
    void commentInteractionTypesAreDiscoveredFromFilteredData() throws Exception {
        mockMvc.perform(get("/api/v2/analytics/comments/types").param("platform", "douyin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].interactionType").value("comment"))
                .andExpect(jsonPath("$.data[0].interactionCount").value(2))
                .andExpect(jsonPath("$.data[1].interactionType").value("reply"));

        mockMvc.perform(get("/api/v2/analytics/comments/types")
                        .param("platform", "iqiyi")
                        .param("startDate", "2026-05-26")
                        .param("endDate", "2026-05-26"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].interactionType").value("review"))
                .andExpect(jsonPath("$.data[0].interactionCount").value(1));
    }

    @Test
    void commentInsightTrendsSupportPlatformTypeAndDateRange() throws Exception {
        mockMvc.perform(get("/api/v2/analytics/comments/trends")
                        .param("platform", "iqiyi")
                        .param("type", "review")
                        .param("startDate", "2026-05-25")
                        .param("endDate", "2026-05-26"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].negativeRatio").value(1.0))
                .andExpect(jsonPath("$.data[1].negativeRatio").value(0.0));
    }

    @Test
    void negativeInteractionsAreFilteredAndPaged() throws Exception {
        mockMvc.perform(get("/api/v2/analytics/comments/negative")
                        .param("platform", "iqiyi")
                        .param("type", "review")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].platformCode").value("iqiyi"))
                .andExpect(jsonPath("$.data.items[0].contentTitle").value("星河计划 第一集"))
                .andExpect(jsonPath("$.data.total").value(1));

        mockMvc.perform(get("/api/v2/analytics/comments/negative")
                        .param("page", "1")
                        .param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.total").value(4));
    }

    @Test
    @Transactional
    void completedOrIgnoredNegativeInteractionTasksAreExcluded() throws Exception {
        jdbcClient.sql("""
                        insert into fact_interaction
                        (interaction_id, content_id, platform_code, external_interaction_id, interaction_type,
                         user_name, text, like_count, captured_at, batch_id, raw_attributes)
                        values (12, 1, 'bilibili', 'BILI_C3', 'comment', 'test-user', 'handled negative sample',
                                1, '2026-05-26 10:00:00', 'mock-batch', '{}')
                        """).update();
        jdbcClient.sql("""
                        insert into fact_text_analysis
                        (analysis_id, interaction_id, sentiment_score, sentiment_label, model_version, created_at)
                        values (12, 12, 0.1, 'negative', 'mock-v1', '2026-05-26 10:05:00')
                        """).update();
        jdbcClient.sql("""
                        insert into ops_task
                        (task_id, title, level, type, text, content_id, platform_code, external_content_id, source, sort_no, is_active)
                        values ('manual-negative-interaction-12', 'handled', 'risk', 'danger', 'handled', 1, 'bilibili', 'BV003', 'manual', 15, 1)
                        """).update();
        jdbcClient.sql("""
                        insert into ops_task_status (task_id, status)
                        values ('manual-negative-interaction-12', 'done')
                        """).update();

        mockMvc.perform(get("/api/v2/analytics/comments/negative").param("page", "1").param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].interactionId", org.hamcrest.Matchers.not(hasItem(12))))
                .andExpect(jsonPath("$.data.total").value(4));
    }

    @Test
    void commentInsightsRejectDanmakuType() throws Exception {
        mockMvc.perform(get("/api/v2/analytics/comments/summary").param("type", "danmaku"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}
