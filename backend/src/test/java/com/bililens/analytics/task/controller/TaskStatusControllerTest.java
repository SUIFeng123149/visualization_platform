package com.bililens.analytics.task.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void taskStatusesCanBeCreatedAndListed() throws Exception {
        mockMvc.perform(put("/api/tasks/statuses/test-task-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"doing\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.taskId").value("test-task-1"))
                .andExpect(jsonPath("$.data.status").value("doing"));

        mockMvc.perform(get("/api/tasks/statuses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[*].taskId", hasItem("test-task-1")));
    }

    @Test
    void tasksReturnPersistedTaskContentWithStatus() throws Exception {
        jdbcClient.sql("""
                        insert into ops_task (task_id, title, level, type, text, bvid, source, sort_no, is_active)
                        values ('task-list-test', '测试任务', '高优先级', 'warning', '测试说明', 'BV003', 'test', 1, 1)
                        """)
                .update();

        mockMvc.perform(put("/api/tasks/statuses/task-list-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"doing\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].taskId").value("task-list-test"))
                .andExpect(jsonPath("$.data[0].title").value("测试任务"))
                .andExpect(jsonPath("$.data[0].status").value("doing"));
    }

    @Test
    void refreshTasksGeneratesTasksFromAnalysisTables() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/tasks/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[*].source", hasItem("auto")))
                .andExpect(jsonPath("$.data[*].taskId", hasItem("auto-review-top-video-BV003")));
    }

    @Test
    void taskStatusCanBeUpdated() throws Exception {
        mockMvc.perform(put("/api/tasks/statuses/test-task-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"doing\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/tasks/statuses/test-task-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"done\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").value("test-task-2"))
                .andExpect(jsonPath("$.data.status").value("done"));
    }

    @Test
    void taskStatusRejectsInvalidStatus() throws Exception {
        mockMvc.perform(put("/api/tasks/statuses/test-task-3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"bad\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}
