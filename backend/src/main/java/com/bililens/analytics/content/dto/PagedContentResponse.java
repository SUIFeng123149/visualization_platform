package com.bililens.analytics.content.dto;

import java.util.List;

public record PagedContentResponse(
        List<ContentSummaryDto> items,
        int page,
        int pageSize,
        long total
) {}
