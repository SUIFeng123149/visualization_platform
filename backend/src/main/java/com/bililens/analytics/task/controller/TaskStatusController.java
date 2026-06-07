package com.bililens.analytics.task.controller;

import com.bililens.analytics.common.ApiResponse;
import com.bililens.analytics.task.dto.TaskDto;
import com.bililens.analytics.task.dto.TaskStatusDto;
import com.bililens.analytics.task.dto.TaskStatusUpdateRequest;
import com.bililens.analytics.task.service.TaskStatusService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
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
@RequestMapping("/api/tasks")
public class TaskStatusController {

    private final TaskStatusService taskStatusService;

    public TaskStatusController(TaskStatusService taskStatusService) {
        this.taskStatusService = taskStatusService;
    }

    @GetMapping("/statuses")
    public ApiResponse<List<TaskStatusDto>> getTaskStatuses() {
        return ApiResponse.ok(taskStatusService.getTaskStatuses());
    }

    @GetMapping
    public ApiResponse<List<TaskDto>> getTasks() {
        return ApiResponse.ok(taskStatusService.getTasks());
    }

    @PostMapping("/refresh")
    public ApiResponse<List<TaskDto>> refreshTasks() {
        return ApiResponse.ok(taskStatusService.refreshGeneratedTasks());
    }

    @PutMapping("/statuses/{taskId}")
    public ApiResponse<TaskStatusDto> updateTaskStatus(
            @PathVariable @Size(max = 255) String taskId,
            @Valid @RequestBody TaskStatusUpdateRequest request
    ) {
        return ApiResponse.ok(taskStatusService.updateTaskStatus(taskId, request.status()));
    }
}
