package com.bililens.analytics.analysis.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "analytics.legacy.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // ==================== video heat-rank ====================

    @Test
    void heatRankReturnsDocumentedResponseShape() throws Exception {
        mockMvc.perform(get("/api/analysis/videos/heat-rank").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].bvid").value("BV003"))
                .andExpect(jsonPath("$.data[0].upName").value("数据小助手"))
                .andExpect(jsonPath("$.data[0].heatScore").value(85920.0));
    }

    @Test
    void heatRankUsesDefaultLimit() throws Exception {
        mockMvc.perform(get("/api/analysis/videos/heat-rank"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(lessThanOrEqualTo(10))));
    }

    @Test
    void heatRankRejectsLimitBelowOne() throws Exception {
        mockMvc.perform(get("/api/analysis/videos/heat-rank").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void heatRankRejectsLimitAboveMax() throws Exception {
        mockMvc.perform(get("/api/analysis/videos/heat-rank").param("limit", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ==================== video sentiment ====================

    @Test
    void videoSentimentsReturnsAll() throws Exception {
        mockMvc.perform(get("/api/analysis/videos/sentiment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(4)))
                .andExpect(jsonPath("$.data[0].bvid").exists())
                .andExpect(jsonPath("$.data[0].positiveRatio").exists())
                .andExpect(jsonPath("$.data[0].negativeRatio").exists());
    }

    @Test
    void videoSentimentsCanBeFilteredByBvids() throws Exception {
        mockMvc.perform(get("/api/analysis/videos/sentiment/by-bvids")
                        .param("bvids", "BV001,BV003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[*].bvid", containsInAnyOrder("BV001", "BV003")));
    }

    @Test
    void videoDetailReturnsDrillDownPayload() throws Exception {
        mockMvc.perform(get("/api/analysis/videos/BV003/detail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.video.bvid").value("BV003"))
                .andExpect(jsonPath("$.data.sentiment.bvid").value("BV003"))
                .andExpect(jsonPath("$.data.danmakuTimeline", hasSize(3)))
                .andExpect(jsonPath("$.data.keywords", hasSize(1)))
                .andExpect(jsonPath("$.data.insights", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data.insights[0].title").exists());
    }

    @Test
    void videoDetailRejectsUnknownBvid() throws Exception {
        mockMvc.perform(get("/api/analysis/videos/BV_UNKNOWN/detail"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ==================== sentiment trend ====================

    @Test
    void sentimentTrendSupportsDateRange() throws Exception {
        mockMvc.perform(get("/api/analysis/sentiment/trend")
                        .param("startDate", "2026-05-22")
                        .param("endDate", "2026-05-24"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].statDate").value("2026-05-22"));
    }

    @Test
    void sentimentTrendRejectsInvalidDateRange() throws Exception {
        mockMvc.perform(get("/api/analysis/sentiment/trend")
                        .param("startDate", "2026-05-24")
                        .param("endDate", "2026-05-22"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("startDate must be earlier than or equal to endDate"));
    }

    @Test
    void sentimentTrendWithoutDatesReturnsAll() throws Exception {
        mockMvc.perform(get("/api/analysis/sentiment/trend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(7)));
    }

    // ==================== danmaku timeline ====================

    @Test
    void danmakuTimelineReturnsAllWhenNoBvid() throws Exception {
        mockMvc.perform(get("/api/analysis/danmaku/timeline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(9)))
                .andExpect(jsonPath("$.data[0].bvid").exists())
                .andExpect(jsonPath("$.data[0].timeBucket").exists());
    }

    @Test
    void danmakuTimelineFiltersByBvid() throws Exception {
        mockMvc.perform(get("/api/analysis/danmaku/timeline").param("bvid", "BV003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].bvid").value("BV003"));
    }

    // ==================== keywords ====================

    @Test
    void keywordsReturnsGlobalByDefault() throws Exception {
        mockMvc.perform(get("/api/analysis/keywords"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data[0].word").exists())
                .andExpect(jsonPath("$.data[0].wordCount").exists());
    }

    @Test
    void keywordsFiltersByBvidDimension() throws Exception {
        mockMvc.perform(get("/api/analysis/keywords")
                        .param("dimensionType", "bvid")
                        .param("dimensionValue", "BV001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    void keywordsFallbackToTimelineTopWordsWhenBvidDimensionMissing() throws Exception {
        mockMvc.perform(get("/api/analysis/keywords")
                        .param("dimensionType", "bvid")
                        .param("dimensionValue", "BV002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data[0].word").exists());
    }

    // ==================== up performance ====================

    @Test
    void upPerformanceReturnsList() throws Exception {
        mockMvc.perform(get("/api/analysis/ups/performance").param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].upName").exists())
                .andExpect(jsonPath("$.data[0].avgHeatScore").exists())
                .andExpect(jsonPath("$.data[0].videoCount").exists());
    }

    @Test
    void upPerformanceRejectsLimitBelowOne() throws Exception {
        mockMvc.perform(get("/api/analysis/ups/performance").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ==================== negative comments ====================

    @Test
    void negativeCommentsCanBeFilteredByBvid() throws Exception {
        mockMvc.perform(get("/api/analysis/comments/negative")
                        .param("bvid", "BV001")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].rpid").value("R001"))
                .andExpect(jsonPath("$.data[0].userName").value("课代表一号"));
    }

    @Test
    void negativeCommentsWithoutBvidReturnsAll() throws Exception {
        mockMvc.perform(get("/api/analysis/comments/negative").param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    void negativeCommentsRejectsLimitAboveMax() throws Exception {
        mockMvc.perform(get("/api/analysis/comments/negative").param("limit", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

}
