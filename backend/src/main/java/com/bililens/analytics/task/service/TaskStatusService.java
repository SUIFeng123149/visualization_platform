package com.bililens.analytics.task.service;

import com.bililens.analytics.content.dto.ContentSummaryDto;
import com.bililens.analytics.content.dto.SentimentSummaryDto;
import com.bililens.analytics.content.dto.TimelinePointDto;
import com.bililens.analytics.content.repository.ContentRepository;
import com.bililens.analytics.task.dto.TaskDto;
import com.bililens.analytics.task.dto.TaskStatusDto;
import com.bililens.analytics.task.repository.TaskStatusRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class TaskStatusService {

    private final TaskStatusRepository taskStatusRepository;
    private final ContentRepository contentRepository;

    public TaskStatusService(TaskStatusRepository taskStatusRepository, ContentRepository contentRepository) {
        this.taskStatusRepository = taskStatusRepository;
        this.contentRepository = contentRepository;
    }

    public List<TaskStatusDto> getTaskStatuses() {
        return taskStatusRepository.findAll();
    }

    public List<TaskDto> getTasks() {
        return taskStatusRepository.findActiveTasks();
    }

    public List<TaskDto> refreshGeneratedTasks() {
        taskStatusRepository.deactivateAutoTasks();
        buildGeneratedTasks().forEach(taskStatusRepository::upsertTask);
        return taskStatusRepository.findActiveTasks();
    }

    public TaskStatusDto updateTaskStatus(String taskId, String status) {
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("taskId is required");
        }
        if (!taskStatusRepository.existsTask(taskId)) {
            throw new IllegalArgumentException("运营任务不存在: " + taskId);
        }
        return taskStatusRepository.upsert(taskId, status);
    }

    private List<TaskDto> buildGeneratedTasks() {
        List<ContentSummaryDto> contents = contentRepository.findContents(null, null, 50);
        List<ContentSentiment> sentiments = contents.stream()
                .map(content -> new ContentSentiment(content, contentRepository.findSentiment(content.contentId())))
                .toList();
        List<TaskDto> tasks = new ArrayList<>();

        contents.stream().findFirst().ifPresent(content -> tasks.add(task(
                "auto-review-content-" + content.contentId(),
                "优先复盘高热内容", "高优先级", "warning",
                content.title() + " 的归一化热度领先，建议复盘互动、时间轴和文本反馈。",
                content, 10
        )));

        sentiments.stream()
                .filter(item -> item.sentiment().negativeRatio() >= 0.25)
                .max(Comparator.comparingDouble(item -> item.sentiment().negativeRatio()))
                .ifPresent(item -> tasks.add(task(
                        "auto-negative-risk-content-" + item.content().contentId(),
                        "处理负向反馈风险", "风险", "danger",
                        item.content().title() + " 的负向反馈占比为 " + formatPercent(item.sentiment().negativeRatio())
                                + "，建议查看高影响互动并制定回应。",
                        item.content(), 20
                )));

        contents.stream()
                .flatMap(content -> contentRepository.findTimeline(content.contentId(), "danmaku").stream()
                        .map(point -> new ContentTimeline(content, point)))
                .max(Comparator.comparingLong(item -> item.timeline().interactionCount()))
                .ifPresent(item -> tasks.add(task(
                        "auto-danmaku-hotspot-content-" + item.content().contentId(),
                        "复盘互动高峰片段", "可执行", "primary",
                        item.content().title() + " 在 " + formatVideoTime(item.timeline().timeBucket())
                                + " 附近出现互动高峰，建议回看相邻片段。",
                        item.content(), 30
                )));

        sentiments.stream()
                .filter(item -> item.sentiment().totalCount() > 0 && item.sentiment().positiveRatio() >= 0.6)
                .max(Comparator.comparingDouble(item -> item.sentiment().positiveRatio()))
                .ifPresent(item -> tasks.add(task(
                        "auto-positive-sample-content-" + item.content().contentId(),
                        "沉淀正向内容样本", "增长", "success",
                        item.content().title() + " 的正向反馈占比为 " + formatPercent(item.sentiment().positiveRatio())
                                + "，建议沉淀为可复用内容样本。",
                        item.content(), 40
                )));

        return tasks;
    }

    private static TaskDto task(String taskId, String title, String level, String type, String text,
                                ContentSummaryDto content, int sortNo) {
        return new TaskDto(taskId, title, level, type, text, content.contentId(), content.platformCode(),
                content.externalContentId(), null, "auto", sortNo, "todo", null,
                LocalDateTime.now(), LocalDateTime.now());
    }

    private static String formatPercent(double value) {
        return String.format("%.1f%%", value * 100);
    }

    private static String formatVideoTime(int seconds) {
        return seconds / 60 + ":" + String.format("%02d", seconds % 60);
    }

    private record ContentSentiment(ContentSummaryDto content, SentimentSummaryDto sentiment) { }

    private record ContentTimeline(ContentSummaryDto content, TimelinePointDto timeline) { }
}
