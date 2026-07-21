package com.bililens.analytics.collector.controller;

import com.bililens.analytics.collector.dto.CollectorTaskCreateRequest;
import com.bililens.analytics.collector.dto.CollectorTaskDto;
import com.bililens.analytics.collector.dto.CollectorTaskPullRequest;
import com.bililens.analytics.collector.dto.CollectorTaskStatusUpdateRequest;
import com.bililens.analytics.collector.service.CollectorTaskService;
import com.bililens.analytics.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/collector/tasks")
public class CollectorTaskController {

    private final CollectorTaskService collectorTaskService;

    public CollectorTaskController(CollectorTaskService collectorTaskService) {
        this.collectorTaskService = collectorTaskService;
    }

    @PostMapping
    public ApiResponse<CollectorTaskDto> createTask(@Valid @RequestBody CollectorTaskCreateRequest request) {
        return ApiResponse.ok(collectorTaskService.createTask(request));
    }

    @GetMapping
    public ApiResponse<List<CollectorTaskDto>> listTasks() {
        return ApiResponse.ok(collectorTaskService.listTasks());
    }

    @GetMapping("/{taskId}")
    public ApiResponse<CollectorTaskDto> getTask(@PathVariable String taskId) {
        return ApiResponse.ok(collectorTaskService.getTask(taskId));
    }

    @PostMapping("/pull")
    public ApiResponse<List<CollectorTaskDto>> pullTasks(@Valid @RequestBody CollectorTaskPullRequest request) {
        return ApiResponse.ok(collectorTaskService.pullTasks(request));
    }

    @PutMapping("/{taskId}/status")
    public ApiResponse<CollectorTaskDto> updateTaskStatus(
            @PathVariable String taskId,
            @Valid @RequestBody CollectorTaskStatusUpdateRequest request
    ) {
        return ApiResponse.ok(collectorTaskService.updateTaskStatus(taskId, request));
    }

    @PostMapping("/{taskId}/cancel")
    public ApiResponse<CollectorTaskDto> cancelTask(@PathVariable String taskId) {
        return ApiResponse.ok(collectorTaskService.cancelTask(taskId));
    }
}
