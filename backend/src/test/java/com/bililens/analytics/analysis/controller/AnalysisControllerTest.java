package com.bililens.analytics.analysis.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
}
