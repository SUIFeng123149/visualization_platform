package com.bililens.analytics.collector.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CollectorTaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void collectorTaskCanBeCreatedPulledAndUpdated() throws Exception {
        String createResponse = mockMvc.perform(post("/api/collector/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "taskName": "测试采集任务",
                                  "naturalLanguage": "采集 B 站 AI 工具视频评论",
                                  "sourceType": "bilibili",
                                  "collectMode": "crawler_ocr_hybrid",
                                  "keywords": ["AI工具", "数据分析"],
                                  "collectPopular": true,
                                  "maxVideos": 10,
                                  "maxCommentsPerVideo": 20,
                                  "ocrEnabled": true,
                                  "screenCaptureEnabled": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.taskId", startsWith("crawl-")))
                .andExpect(jsonPath("$.data.status").value("pending"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String taskId = createResponse.replaceFirst("(?s).*\"taskId\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        mockMvc.perform(post("/api/collector/tasks/pull")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workerId\":\"ubuntu-collector-1\",\"limit\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].taskId").value(taskId))
                .andExpect(jsonPath("$.data[0].status").value("dispatched"));

        mockMvc.perform(put("/api/collector/tasks/" + taskId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "success",
                                  "progress": 100,
                                  "externalTaskId": "py-crawl-001",
                                  "message": "采集、分析、入库完成",
                                  "rawHdfsPath": "hdfs://cluster/raw/crawl-001",
                                  "cleanHdfsPath": "hdfs://cluster/clean/crawl-001",
                                  "batchId": "batch-001",
                                  "resultTables": ["ads_video_heat_rank", "ads_video_sentiment"],
                                  "rowCount": 128
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("success"))
                .andExpect(jsonPath("$.data.rawHdfsPath").value("hdfs://cluster/raw/crawl-001"))
                .andExpect(jsonPath("$.data.resultTables", hasItem("ads_video_sentiment")))
                .andExpect(jsonPath("$.data.rowCount").value(128));

        mockMvc.perform(get("/api/collector/tasks/" + taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.externalTaskId").value("py-crawl-001"))
                .andExpect(jsonPath("$.data.batchId").value("batch-001"));
    }

    @Test
    void collectorTaskRejectsEmptySourceAndNaturalLanguage() throws Exception {
        mockMvc.perform(post("/api/collector/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskName\":\"空任务\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}
