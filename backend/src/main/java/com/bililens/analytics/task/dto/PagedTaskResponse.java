package com.bililens.analytics.task.dto;

import java.util.List;

public record PagedTaskResponse(
        List<TaskDto> items,
        int page,
        int pageSize,
        long total
) {
}
