package com.bililens.analytics.platform.service;

import com.bililens.analytics.platform.dto.AnomalyRuleDto;
import com.bililens.analytics.platform.dto.AnomalyRuleUpdateRequest;
import com.bililens.analytics.platform.dto.DataSourceStatusDto;
import com.bililens.analytics.platform.dto.ReportCreateRequest;
import com.bililens.analytics.platform.dto.ReportHistoryDto;
import com.bililens.analytics.platform.repository.PlatformRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlatformService {

    private final PlatformRepository platformRepository;

    public PlatformService(PlatformRepository platformRepository) {
        this.platformRepository = platformRepository;
    }

    public List<DataSourceStatusDto> getDataSourceStatuses() {
        return platformRepository.findDataSourceStatuses();
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
