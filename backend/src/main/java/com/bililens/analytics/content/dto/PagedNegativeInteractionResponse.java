package com.bililens.analytics.content.dto;

import java.util.List;

public record PagedNegativeInteractionResponse(
        List<NegativeInteractionDto> items,
        int page,
        int pageSize,
        long total
) {
}
