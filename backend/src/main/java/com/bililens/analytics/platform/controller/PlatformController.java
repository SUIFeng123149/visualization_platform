package com.bililens.analytics.platform.controller;

import com.bililens.analytics.common.ApiResponse;
import com.bililens.analytics.platform.dto.AnomalyRuleDto;
import com.bililens.analytics.platform.dto.AnomalyRuleUpdateRequest;
import com.bililens.analytics.platform.dto.DataSourceStatusDto;
import com.bililens.analytics.platform.dto.ReportCreateRequest;
import com.bililens.analytics.platform.dto.ReportHistoryDto;
import com.bililens.analytics.platform.dto.PlatformConfigDto;
import com.bililens.analytics.platform.dto.PlatformConfigRequest;
import com.bililens.analytics.platform.dto.PlatformValidationDto;
import com.bililens.analytics.platform.dto.PlatformValidationRequest;
import com.bililens.analytics.platform.dto.MetricConfigDto;
import com.bililens.analytics.platform.dto.MetricConfigRequest;
import com.bililens.analytics.platform.dto.PasswordConfirmRequest;
import com.bililens.analytics.platform.service.PlatformService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;

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

    @GetMapping("/platforms")
    public ApiResponse<List<PlatformConfigDto>> getPlatformConfigs() { return ApiResponse.ok(platformService.getPlatformConfigs()); }

    @PutMapping("/platforms")
    public ApiResponse<PlatformConfigDto> savePlatformConfig(@Valid @RequestBody PlatformConfigRequest request) { return ApiResponse.ok(platformService.savePlatformConfig(request)); }

    @PostMapping("/platforms/validate")
    public ApiResponse<PlatformValidationDto> validatePlatformCodes(@Valid @RequestBody PlatformValidationRequest request) {
        return ApiResponse.ok(platformService.validatePlatformCodes(request.platformCodes()));
    }

    @DeleteMapping("/platforms/{platformCode}")
    public ApiResponse<Void> deletePlatformConfig(@PathVariable @Size(max = 32) String platformCode,
                                                   @Valid @RequestBody PasswordConfirmRequest request) {
        platformService.deletePlatformConfig(platformCode, request.password());
        return ApiResponse.ok(null);
    }

    @GetMapping("/metrics")
    public ApiResponse<List<MetricConfigDto>> getMetricConfigs() { return ApiResponse.ok(platformService.getMetricConfigs()); }

    @PutMapping("/metrics/{metricKey}")
    public ApiResponse<MetricConfigDto> saveMetricConfig(@PathVariable @Size(max = 64) String metricKey, @Valid @RequestBody MetricConfigRequest request) { return ApiResponse.ok(platformService.saveMetricConfig(metricKey, request)); }

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
