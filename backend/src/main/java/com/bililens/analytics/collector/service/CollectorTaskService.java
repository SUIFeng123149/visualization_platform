package com.bililens.analytics.collector.service;

import com.bililens.analytics.collector.dto.CollectorTaskCreateRequest;
import com.bililens.analytics.collector.dto.CollectorTaskDto;
import com.bililens.analytics.collector.dto.CollectorTaskPullRequest;
import com.bililens.analytics.collector.dto.CollectorTaskStatusUpdateRequest;
import com.bililens.analytics.collector.repository.CollectorTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class CollectorTaskService {

    private static final List<String> ALLOWED_STATUSES = List.of(
            "pending", "dispatched", "running", "uploaded", "analyzing", "importing", "success", "failed", "cancelled"
    );

    private final CollectorTaskRepository collectorTaskRepository;

    public CollectorTaskService(CollectorTaskRepository collectorTaskRepository) {
        this.collectorTaskRepository = collectorTaskRepository;
    }

    public CollectorTaskDto createTask(CollectorTaskCreateRequest request) {
        validateCreateRequest(request);
        LocalDateTime now = LocalDateTime.now();
        String taskId = "crawl-" + now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "-" + UUID.randomUUID().toString().substring(0, 8);
        String paramsJson = buildParamsJson(request);
        CollectorTaskDto task = new CollectorTaskDto(
                taskId,
                null,
                defaultText(request.taskName(), "自然语言数据采集任务"),
                request.naturalLanguage(),
                defaultText(request.sourceType(), "bilibili"),
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
        if (!hasNaturalLanguage && !hasPopular && !hasHomepage && !hasMid && !hasKeyword) {
            throw new IllegalArgumentException("请填写自然语言需求或至少选择一个采集来源");
        }
    }

    private static String buildParamsJson(CollectorTaskCreateRequest request) {
        List<String> keywords = request.keywords() == null
                ? List.of()
                : request.keywords().stream().filter(StringUtils::hasText).map(String::trim).toList();
        List<Long> mids = request.mids() == null ? List.of() : request.mids();
        return """
                {
                  "keywords": %s,
                  "mids": %s,
                  "collectPopular": %s,
                  "collectHomepage": %s,
                  "maxVideos": %s,
                  "maxCommentsPerVideo": %s,
                  "workers": %s,
                  "commentDelay": %s,
                  "danmakuDelay": %s,
                  "ocr": {
                    "enabled": %s,
                    "screenCaptureEnabled": %s,
                    "language": "%s",
                    "minConfidence": %s
                  }
                }
                """.formatted(
                toJsonArray(keywords),
                mids,
                Boolean.TRUE.equals(request.collectPopular()),
                Boolean.TRUE.equals(request.collectHomepage()),
                request.maxVideos() == null ? 100 : request.maxVideos(),
                request.maxCommentsPerVideo() == null ? 50 : request.maxCommentsPerVideo(),
                request.workers() == null ? 2 : request.workers(),
                request.commentDelay() == null ? 1.0 : request.commentDelay(),
                request.danmakuDelay() == null ? 6.0 : request.danmakuDelay(),
                Boolean.TRUE.equals(request.ocrEnabled()),
                Boolean.TRUE.equals(request.screenCaptureEnabled()),
                escapeJson(defaultText(request.ocrLanguage(), "ch")),
                request.minOcrConfidence() == null ? 0.7 : request.minOcrConfidence()
        );
    }

    private static String toJsonArray(List<String> values) {
        return "[" + values.stream()
                .map(value -> "\"" + escapeJson(value) + "\"")
                .reduce((a, b) -> a + ", " + b)
                .orElse("") + "]";
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
