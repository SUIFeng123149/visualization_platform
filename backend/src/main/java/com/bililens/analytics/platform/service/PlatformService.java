package com.bililens.analytics.platform.service;

import com.bililens.analytics.platform.dto.AnomalyRuleDto;
import com.bililens.analytics.platform.dto.AnomalyRuleUpdateRequest;
import com.bililens.analytics.platform.dto.DataSourceStatusDto;
import com.bililens.analytics.platform.dto.ReportCreateRequest;
import com.bililens.analytics.platform.dto.ReportHistoryDto;
import com.bililens.analytics.platform.dto.PlatformConfigDto;
import com.bililens.analytics.platform.dto.PlatformConfigRequest;
import com.bililens.analytics.platform.dto.MetricConfigDto;
import com.bililens.analytics.platform.dto.MetricConfigRequest;
import com.bililens.analytics.platform.repository.PlatformRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import java.util.List;

@Service
public class PlatformService {

    private final PlatformRepository platformRepository;
    private final String deletePassword;

    public PlatformService(PlatformRepository platformRepository,
                           @Value("${analytics.platform-admin.delete-password:}") String deletePassword) {
        this.platformRepository = platformRepository;
        this.deletePassword = deletePassword;
    }

    public List<DataSourceStatusDto> getDataSourceStatuses() {
        return platformRepository.findDataSourceStatuses();
    }

    public List<PlatformConfigDto> getPlatformConfigs() { return platformRepository.findPlatformConfigs(); }
    public PlatformConfigDto savePlatformConfig(PlatformConfigRequest request) { return platformRepository.upsertPlatformConfig(request); }
    public List<MetricConfigDto> getMetricConfigs() { return platformRepository.findMetricConfigs(); }
    public MetricConfigDto saveMetricConfig(String metricKey, MetricConfigRequest request) { return platformRepository.updateMetricConfig(metricKey, request); }

    public void deletePlatformConfig(String platformCode, String password) {
        if (deletePassword.isBlank()) throw new IllegalArgumentException("未配置平台删除密码");
        if (!MessageDigest.isEqual(deletePassword.getBytes(StandardCharsets.UTF_8), password.getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("管理密码不正确");
        }
        platformRepository.deletePlatformConfig(platformCode);
    }

    public List<ReportHistoryDto> getReportHistory() {
        return platformRepository.findReportHistory();
    }

    public ReportHistoryDto createReportHistory(ReportCreateRequest request) {
        return platformRepository.createReportHistory(request);
    }

    public List<AnomalyRuleDto> getAnomalyRules() {
        return platformRepository.findAnomalyRules();
    }

    public AnomalyRuleDto updateAnomalyRule(AnomalyRuleUpdateRequest request) {
        return platformRepository.upsertAnomalyRule(request);
    }
}
