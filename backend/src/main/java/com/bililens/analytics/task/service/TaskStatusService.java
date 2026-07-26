package com.bililens.analytics.task.service;

import com.bililens.analytics.content.dto.ContentSummaryDto;
import com.bililens.analytics.content.dto.NegativeInteractionDto;
import com.bililens.analytics.content.dto.SentimentSummaryDto;
import com.bililens.analytics.content.dto.TimelinePointDto;
import com.bililens.analytics.content.repository.ContentRepository;
import com.bililens.analytics.platform.dto.AnomalyRuleDto;
import com.bililens.analytics.platform.repository.PlatformRepository;
import com.bililens.analytics.task.dto.TaskDto;
import com.bililens.analytics.task.dto.PagedTaskResponse;
import com.bililens.analytics.task.dto.TaskStatusDto;
import com.bililens.analytics.task.repository.TaskStatusRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

@Service
public class TaskStatusService {
    private final TaskStatusRepository taskStatusRepository;
    private final ContentRepository contentRepository;
    private final PlatformRepository platformRepository;

    public TaskStatusService(TaskStatusRepository taskStatusRepository, ContentRepository contentRepository, PlatformRepository platformRepository) {
        this.taskStatusRepository = taskStatusRepository;
        this.contentRepository = contentRepository;
        this.platformRepository = platformRepository;
    }

    public List<TaskStatusDto> getTaskStatuses() { return taskStatusRepository.findAll(); }
    public List<TaskDto> getTasks() { return taskStatusRepository.findActiveTasks(); }
    public PagedTaskResponse getTasksPage(String status, int page, int pageSize) {
        if (status != null && !List.of("open", "todo", "doing", "done", "ignored").contains(status)) {
            throw new IllegalArgumentException("Unsupported task status: " + status);
        }
        return taskStatusRepository.findActiveTasks(status, page, pageSize);
    }

    public List<TaskDto> refreshGeneratedTasks() {
        taskStatusRepository.deactivateAutoTasks();
        buildGeneratedTasks().forEach(taskStatusRepository::upsertTask);
        return taskStatusRepository.findActiveTasks();
    }

    public TaskStatusDto updateTaskStatus(String taskId, String status) {
        if (taskId == null || taskId.isBlank()) throw new IllegalArgumentException("taskId is required");
        if (!taskStatusRepository.existsTask(taskId)) throw new IllegalArgumentException("运营任务不存在: " + taskId);
        return taskStatusRepository.upsert(taskId, status);
    }

    public TaskDto createNegativeInteractionTask(long interactionId) {
        NegativeInteractionDto interaction = contentRepository.findNegativeInteraction(interactionId)
                .orElseThrow(() -> new IllegalArgumentException("Negative interaction not found: " + interactionId));
        String taskId = "manual-negative-interaction-" + interaction.interactionId();
        TaskDto task = new TaskDto(
                taskId,
                "处理负向互动",
                "风险",
                "danger",
                negativeInteractionTaskText(interaction),
                interaction.contentId(),
                interaction.platformCode(),
                interaction.externalContentId(),
                "manual",
                15,
                "todo",
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        taskStatusRepository.upsertTask(task);
        return taskStatusRepository.findActiveTasks().stream()
                .filter(item -> item.taskId().equals(taskId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Failed to create negative interaction task"));
    }

    private List<TaskDto> buildGeneratedTasks() {
        List<ContentSummaryDto> contents = contentRepository.findContents(null, null, 50);
        Map<String, AnomalyRuleDto> rules = platformRepository.findAnomalyRules().stream()
                .filter(AnomalyRuleDto::enabled)
                .collect(Collectors.toMap(AnomalyRuleDto::ruleKey, rule -> rule, (left, right) -> right));
        List<ContentSentiment> sentiments = contents.stream()
                .map(content -> new ContentSentiment(content, contentRepository.findSentiment(content.contentId())))
                .toList();
        List<TaskDto> tasks = new ArrayList<>();

        addTopContentTask(tasks, rules.get("heat_top"), contents, "auto-review-content-", "优先复盘高热内容", "平台热度为 ", "，建议复盘互动、时间轴和文本反馈。", 10, TaskStatusService::platformHeatScore);

        AnomalyRuleDto negativeRule = rules.get("negative_risk");
        if (negativeRule != null) {
            sentiments.stream().filter(item -> item.sentiment().totalCount() > 0 && matches(negativeRule, item.sentiment().negativeRatio()))
                    .max(Comparator.comparingDouble(item -> item.sentiment().negativeRatio()))
                    .ifPresent(item -> tasks.add(task("auto-negative-risk-content-" + item.content().contentId(), "处理负向反馈风险", negativeRule,
                            item.content().title() + " 的负向反馈占比为 " + formatPercent(item.sentiment().negativeRatio()) + "，已触发规则阈值 " + formatPercent(negativeRule.threshold()) + "，建议查看高影响互动并制定回应。", item.content(), 20)));
        }

        AnomalyRuleDto danmakuRule = rules.get("danmaku_hotspot");
        if (danmakuRule != null) {
            contents.stream().flatMap(content -> contentRepository.findTimeline(content.contentId(), "danmaku").stream().map(point -> new ContentTimeline(content, point)))
                    .filter(item -> matches(danmakuRule, item.timeline().interactionCount()))
                    .max(Comparator.comparingLong(item -> item.timeline().interactionCount()))
                    .ifPresent(item -> tasks.add(task("auto-danmaku-hotspot-content-" + item.content().contentId(), "复盘互动高峰片段", danmakuRule,
                            item.content().title() + " 在 " + formatVideoTime(item.timeline().timeBucket()) + " 附近的互动数为 " + item.timeline().interactionCount() + "，已触发规则阈值 " + trimNumber(danmakuRule.threshold()) + "，建议回看相邻片段。", item.content(), 30)));
        }

        addTopContentTask(tasks, rules.get("interaction_high"), contents, "auto-interaction-high-content-", "沉淀高互动内容样本", "互动率为 ", "，建议沉淀为可复用内容样本。", 40, TaskStatusService::interactionRate);
        return tasks;
    }

    private static void addTopContentTask(List<TaskDto> tasks, AnomalyRuleDto rule, List<ContentSummaryDto> contents, String taskPrefix, String title, String valuePrefix, String suffix, int sortNo, ToDoubleFunction<ContentSummaryDto> extractor) {
        if (rule == null) return;
        contents.stream().filter(content -> matches(rule, extractor.applyAsDouble(content))).max(Comparator.comparingDouble(extractor))
                .ifPresent(content -> tasks.add(task(taskPrefix + content.contentId(), title, rule,
                        content.title() + " 的" + valuePrefix + formatRuleValue(rule.metric(), extractor.applyAsDouble(content)) + "，已触发规则阈值 " + formatRuleValue(rule.metric(), rule.threshold()) + suffix, content, sortNo)));
    }

    private static TaskDto task(String taskId, String title, AnomalyRuleDto rule, String text, ContentSummaryDto content, int sortNo) {
        return new TaskDto(taskId, title, levelLabel(rule.level()), rule.level(), text, content.contentId(), content.platformCode(), content.externalContentId(), "auto", sortNo, "todo", null, LocalDateTime.now(), LocalDateTime.now());
    }

    private static boolean matches(AnomalyRuleDto rule, double value) {
        return switch (rule.operator()) {
            case ">" -> value > rule.threshold(); case ">=" -> value >= rule.threshold(); case "<" -> value < rule.threshold(); case "<=" -> value <= rule.threshold(); case "=", "==" -> Double.compare(value, rule.threshold()) == 0; default -> false;
        };
    }
    private static double platformHeatScore(ContentSummaryDto content) { return content.platformHeatScore() == null ? Double.NEGATIVE_INFINITY : content.platformHeatScore(); }
    private static double interactionRate(ContentSummaryDto content) { if (content.viewCount() == null || content.viewCount() <= 0) return 0; return (double) (value(content.likeCount()) + value(content.commentCount()) + value(content.shareCount()) + value(content.favoriteCount()) + value(content.danmakuCount()) + value(content.coinCount())) / content.viewCount(); }
    private static long value(Long number) { return number == null ? 0L : number; }
    private static String levelLabel(String level) { return Map.of("danger", "风险", "warning", "预警", "success", "增长", "primary", "提示").getOrDefault(level, "提示"); }
    private static String formatRuleValue(String metric, double value) { return metric.toLowerCase().contains("ratio") ? formatPercent(value) : trimNumber(value); }
    private static String trimNumber(double value) { return value == Math.rint(value) ? String.format("%.0f", value) : String.format("%.3f", value); }
    private static String formatPercent(double value) { return String.format("%.1f%%", value * 100); }
    private static String formatVideoTime(int seconds) { return seconds / 60 + ":" + String.format("%02d", seconds % 60); }
    private static String negativeInteractionTaskText(NegativeInteractionDto interaction) {
        String text = interaction.text() == null ? "" : interaction.text().replaceAll("\\s+", " ").trim();
        if (text.length() > 240) text = text.substring(0, 240) + "...";
        String user = interaction.userName() == null || interaction.userName().isBlank() ? "匿名用户" : interaction.userName();
        String likes = interaction.likeCount() == null ? "--" : interaction.likeCount().toString();
        return "负向" + interaction.interactionType() + "来自" + user + "，点赞 " + likes
                + "，情感分 " + (interaction.sentimentScore() == null ? "--" : String.format("%.2f", interaction.sentimentScore()))
                + "。原文：" + text + "。请评估问题、制定回应，并在完成后更新处置状态。";
    }
    private record ContentSentiment(ContentSummaryDto content, SentimentSummaryDto sentiment) { }
    private record ContentTimeline(ContentSummaryDto content, TimelinePointDto timeline) { }
}
