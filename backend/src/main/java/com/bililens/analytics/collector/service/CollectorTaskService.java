package com.bililens.analytics.collector.service;

import com.bililens.analytics.collector.dto.CollectorTaskCreateRequest;
import com.bililens.analytics.collector.dto.CollectorTaskDto;
import com.bililens.analytics.collector.dto.CollectorTaskPullRequest;
import com.bililens.analytics.collector.dto.CollectorTaskStatusUpdateRequest;
import com.bililens.analytics.collector.repository.CollectorTaskRepository;
import com.bililens.analytics.content.dto.PlatformDto;
import com.bililens.analytics.content.repository.ContentRepository;
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
    private final ContentRepository contentRepository;
    private final ObjectMapper objectMapper;

    public CollectorTaskService(CollectorTaskRepository collectorTaskRepository, ContentRepository contentRepository,
                                ObjectMapper objectMapper) {
        this.collectorTaskRepository = collectorTaskRepository;
        this.contentRepository = contentRepository;
        this.objectMapper = objectMapper;
    }

    public CollectorTaskDto createTask(CollectorTaskCreateRequest request) {
        validateCreateRequest(request);
        LocalDateTime now = LocalDateTime.now();
        String taskId = "crawl-" + now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "-" + UUID.randomUUID().toString().substring(0, 8);
        CollectionContext context = resolveCollectionContext(request);
        String paramsJson = buildParamsJson(request, context);
        CollectorTaskDto task = new CollectorTaskDto(
                taskId,
                null,
                defaultText(request.taskName(), "自然语言数据采集任务"),
                request.naturalLanguage(),
                context.platformCode(),
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
        boolean hasGenericTarget = request.targets() != null && request.targets().stream().anyMatch(StringUtils::hasText);
        if (!hasNaturalLanguage && !hasGenericTarget) {
            throw new IllegalArgumentException("请填写自然语言需求或至少选择一个采集来源");
        }
    }

    private CollectionContext resolveCollectionContext(CollectorTaskCreateRequest request) {
        String platformCode = defaultText(request.platformCode(), request.sourceType());
        if (!StringUtils.hasText(platformCode)) {
            throw new IllegalArgumentException("采集任务必须指定平台");
        }
        if (StringUtils.hasText(request.platformCode()) && StringUtils.hasText(request.sourceType())
                && !request.platformCode().trim().equals(request.sourceType().trim())) {
            throw new IllegalArgumentException("platformCode 与 sourceType 不一致");
        }

        PlatformDto platform = contentRepository.findPlatforms().stream()
                .filter(item -> item.platformCode().equals(platformCode))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未注册的平台: " + platformCode));
        if (!platform.enabled()) {
            throw new IllegalArgumentException("平台已停用: " + platformCode);
        }

        String connectorName = defaultText(request.connectorName(), platform.connectorName());
        if (!connectorName.equals(platform.connectorName())) {
            throw new IllegalArgumentException("连接器与平台注册配置不一致: " + connectorName);
        }

        String targetType = defaultText(request.targetType(), "content");
        if (!List.of("keyword", "account", "content", "series", "popular").contains(targetType)) {
            throw new IllegalArgumentException("不支持的采集目标类型: " + targetType);
        }
        if ("series".equals(targetType) && !Boolean.TRUE.equals(platform.capabilities().get("series"))) {
            throw new IllegalArgumentException("该平台不支持剧集采集: " + platformCode);
        }

        List<String> requestedCapabilities = request.requestedCapabilities() == null
                ? List.of("content", "metrics")
                : request.requestedCapabilities().stream().filter(StringUtils::hasText).map(String::trim).distinct().toList();
        for (String capability : requestedCapabilities) {
            boolean supported = List.of("content", "metrics").contains(capability)
                    || Boolean.TRUE.equals(platform.capabilities().get(capability));
            if (!supported) {
                throw new IllegalArgumentException("平台 " + platformCode + " 不支持采集能力: " + capability);
            }
        }
        return new CollectionContext(platformCode, connectorName, targetType, requestedCapabilities);
    }

    private String buildParamsJson(CollectorTaskCreateRequest request, CollectionContext context) {
        List<String> targets = request.targets() == null
                ? List.of()
                : request.targets().stream().filter(StringUtils::hasText).map(String::trim).toList();

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("schemaVersion", "2.0");
        params.put("platformCode", context.platformCode());
        params.put("connectorName", context.connectorName());
        params.put("targetType", context.targetType());
        params.put("targets", targets);
        params.put("requestedCapabilities", context.requestedCapabilities());

        Map<String, Object> options = new LinkedHashMap<>();
        if (request.options() != null) {
            options.putAll(request.options());
        }
        options.putIfAbsent("maxContents", request.maxVideos() == null ? 100 : request.maxVideos());
        options.putIfAbsent("maxInteractionsPerContent", request.maxCommentsPerVideo() == null ? 50 : request.maxCommentsPerVideo());
        options.putIfAbsent("workers", request.workers() == null ? 2 : request.workers());
        options.putIfAbsent("requestDelaySeconds", request.commentDelay() == null ? 1.0 : request.commentDelay());
        options.putIfAbsent("ocrEnabled", Boolean.TRUE.equals(request.ocrEnabled()));
        params.put("options", options);

        try {
            return objectMapper.writeValueAsString(params);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("采集参数无法序列化", exception);
        }
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private record CollectionContext(String platformCode, String connectorName, String targetType,
                                     List<String> requestedCapabilities) { }
}
