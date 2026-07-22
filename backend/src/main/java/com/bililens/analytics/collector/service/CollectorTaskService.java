package com.bililens.analytics.collector.service;

import com.bililens.analytics.collector.dto.CollectorTaskCreateRequest;
import com.bililens.analytics.collector.dto.CollectorTaskDto;
import com.bililens.analytics.collector.dto.CollectorTaskPullRequest;
import com.bililens.analytics.collector.dto.CollectorTaskStatusUpdateRequest;
import com.bililens.analytics.collector.repository.CollectorTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CollectorTaskService {

    private static final List<String> ALLOWED_STATUSES = List.of(
            "pending", "dispatched", "running", "uploaded", "analyzing", "importing", "success", "failed", "cancelled"
    );

    private final CollectorTaskRepository collectorTaskRepository;
    private final ObjectMapper objectMapper;

    public CollectorTaskService(CollectorTaskRepository collectorTaskRepository, ObjectMapper objectMapper) {
        this.collectorTaskRepository = collectorTaskRepository;
        this.objectMapper = objectMapper;
    }

    public CollectorTaskDto createTask(CollectorTaskCreateRequest request) {
        validateCreateRequest(request);
        LocalDateTime now = LocalDateTime.now();
        String taskId = "crawl-" + now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "-" + UUID.randomUUID().toString().substring(0, 8);
        String platformCode = defaultText(request.platformCode(), defaultText(request.sourceType(), "bilibili"));
        String paramsJson = buildParamsJson(request, platformCode);
        CollectorTaskDto task = new CollectorTaskDto(
                taskId,
                null,
                defaultText(request.taskName(), "自然语言数据采集任务"),
                request.naturalLanguage(),
                platformCode,
                defaultText(request.collectMode(), "crawler_ocr_hybrid"),
                "pending",
                0,
                defaultText(request.priority(), "normal"),
                "任务已创建，等待采集端领取",
                null,
                null,
                null,
                null,
                List.of(),
                paramsJson,
                request.callbackUrl(),
                now,
                now,
                null,
                null,
                null
        );
        return collectorTaskRepository.create(task);
    }

    public List<CollectorTaskDto> listTasks() {
        return collectorTaskRepository.findRecent();
    }

    public CollectorTaskDto getTask(String taskId) {
        return collectorTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("采集任务不存在"));
    }

    public List<CollectorTaskDto> pullTasks(CollectorTaskPullRequest request) {
        String workerId = defaultText(request.workerId(), "collector-worker");
        int limit = request.limit() == null ? 1 : request.limit();
        return collectorTaskRepository.pullPending(workerId, limit);
    }

    public CollectorTaskDto updateTaskStatus(String taskId, CollectorTaskStatusUpdateRequest request) {
        String status = request.status();
        if (status != null && !ALLOWED_STATUSES.contains(status)) {
            throw new IllegalArgumentException("任务状态不合法: " + status);
        }
        return collectorTaskRepository.updateStatus(taskId, request);
    }

    public CollectorTaskDto cancelTask(String taskId) {
        CollectorTaskDto task = getTask(taskId);
        if (List.of("success", "failed", "cancelled").contains(task.status())) {
            throw new IllegalArgumentException("已结束任务不能取消");
        }
        return collectorTaskRepository.cancel(taskId);
    }

    private static void validateCreateRequest(CollectorTaskCreateRequest request) {
        boolean hasNaturalLanguage = StringUtils.hasText(request.naturalLanguage());
        boolean hasPopular = Boolean.TRUE.equals(request.collectPopular());
        boolean hasHomepage = Boolean.TRUE.equals(request.collectHomepage());
        boolean hasMid = request.mids() != null && !request.mids().isEmpty();
        boolean hasKeyword = request.keywords() != null && request.keywords().stream().anyMatch(StringUtils::hasText);
        boolean hasGenericTarget = request.targets() != null && request.targets().stream().anyMatch(StringUtils::hasText);
        if (!hasNaturalLanguage && !hasPopular && !hasHomepage && !hasMid && !hasKeyword && !hasGenericTarget) {
            throw new IllegalArgumentException("请填写自然语言需求或至少选择一个采集来源");
        }
    }

    private String buildParamsJson(CollectorTaskCreateRequest request, String platformCode) {
        List<String> keywords = request.keywords() == null
                ? List.of()
                : request.keywords().stream().filter(StringUtils::hasText).map(String::trim).toList();
        List<Long> mids = request.mids() == null ? List.of() : request.mids();
        List<String> targets = request.targets() == null
                ? List.of()
                : request.targets().stream().filter(StringUtils::hasText).map(String::trim).toList();
        if (targets.isEmpty()) {
            targets = !keywords.isEmpty() ? keywords : mids.stream().map(String::valueOf).toList();
        }

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("schemaVersion", "2.0");
        params.put("platformCode", platformCode);
        params.put("connectorName", defaultText(request.connectorName(), platformCode + "-default"));
        params.put("targetType", defaultText(request.targetType(), inferLegacyTargetType(request)));
        params.put("targets", targets);
        params.put("requestedCapabilities", request.requestedCapabilities() == null ? List.of("content", "metrics") : request.requestedCapabilities());

        Map<String, Object> options = new LinkedHashMap<>();
        if (request.options() != null) {
            options.putAll(request.options());
        }
        options.putIfAbsent("maxContents", request.maxVideos() == null ? 100 : request.maxVideos());
        options.putIfAbsent("maxInteractionsPerContent", request.maxCommentsPerVideo() == null ? 50 : request.maxCommentsPerVideo());
        options.putIfAbsent("workers", request.workers() == null ? 2 : request.workers());
        options.putIfAbsent("requestDelaySeconds", request.commentDelay() == null ? 1.0 : request.commentDelay());
        options.putIfAbsent("ocrEnabled", Boolean.TRUE.equals(request.ocrEnabled()));
        options.put("legacy", Map.of(
                "keywords", keywords,
                "mids", mids,
                "collectPopular", Boolean.TRUE.equals(request.collectPopular()),
                "collectHomepage", Boolean.TRUE.equals(request.collectHomepage()),
                "danmakuDelay", request.danmakuDelay() == null ? 6.0 : request.danmakuDelay()
        ));
        params.put("options", options);

        try {
            return objectMapper.writeValueAsString(params);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("采集参数无法序列化", exception);
        }
    }

    private static String inferLegacyTargetType(CollectorTaskCreateRequest request) {
        if (Boolean.TRUE.equals(request.collectPopular())) return "popular";
        if (Boolean.TRUE.equals(request.collectHomepage())) return "homepage";
        if (request.mids() != null && !request.mids().isEmpty()) return "account";
        return "keyword";
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
