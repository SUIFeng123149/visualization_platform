package com.bililens.analytics.platform.controller;

import com.bililens.analytics.common.ApiResponse;
import com.bililens.analytics.platform.dto.AnomalyRuleDto;
import com.bililens.analytics.platform.dto.AnomalyRuleUpdateRequest;
import com.bililens.analytics.platform.dto.DataSourceStatusDto;
import com.bililens.analytics.platform.dto.ReportCreateRequest;
import com.bililens.analytics.platform.dto.ReportHistoryDto;
import com.bililens.analytics.platform.service.PlatformService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/platform")
public class PlatformController {

    private final PlatformService platformService;

    public PlatformController(PlatformService platformService) {
        this.platformService = platformService;
    }

    @GetMapping("/data-sources/status")
    public ApiResponse<List<DataSourceStatusDto>> getDataSourceStatuses() {
        return ApiResponse.ok(platformService.getDataSourceStatuses());
    }

    @GetMapping("/reports")
    public ApiResponse<List<ReportHistoryDto>> getReportHistory() {
        return ApiResponse.ok(platformService.getReportHistory());
    }

    @PostMapping("/reports")
    public ApiResponse<ReportHistoryDto> createReportHistory(@Valid @RequestBody ReportCreateRequest request) {
        return ApiResponse.ok(platformService.createReportHistory(request));
    }

    @GetMapping("/anomaly-rules")
    public ApiResponse<List<AnomalyRuleDto>> getAnomalyRules() {
        return ApiResponse.ok(platformService.getAnomalyRules());
    }

    @PutMapping("/anomaly-rules")
    public ApiResponse<AnomalyRuleDto> updateAnomalyRule(@Valid @RequestBody AnomalyRuleUpdateRequest request) {
        return ApiResponse.ok(platformService.updateAnomalyRule(request));
    }
}
