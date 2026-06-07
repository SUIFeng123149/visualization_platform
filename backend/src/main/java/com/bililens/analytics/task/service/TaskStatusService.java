package com.bililens.analytics.task.service;

import com.bililens.analytics.analysis.dto.DanmakuTimelineDto;
import com.bililens.analytics.analysis.dto.UpPerformanceDto;
import com.bililens.analytics.analysis.dto.VideoHeatRankDto;
import com.bililens.analytics.analysis.dto.VideoSentimentDto;
import com.bililens.analytics.analysis.repository.AnalysisRepository;
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
    private final AnalysisRepository analysisRepository;

    public TaskStatusService(TaskStatusRepository taskStatusRepository, AnalysisRepository analysisRepository) {
        this.taskStatusRepository = taskStatusRepository;
        this.analysisRepository = analysisRepository;
    }

    public List<TaskStatusDto> getTaskStatuses() {
        return taskStatusRepository.findAll();
    }

    public List<TaskDto> getTasks() {
        return taskStatusRepository.findActiveTasks();
    }

    public List<TaskDto> refreshGeneratedTasks() {
        List<TaskDto> generatedTasks = buildGeneratedTasks();
        taskStatusRepository.deactivateAutoTasks();
        generatedTasks.forEach(taskStatusRepository::upsertTask);
        return taskStatusRepository.findActiveTasks();
    }

    public TaskStatusDto updateTaskStatus(String taskId, String status) {
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("taskId is required");
        }
        return taskStatusRepository.upsert(taskId, status);
    }

    private List<TaskDto> buildGeneratedTasks() {
        List<VideoHeatRankDto> heatRank = analysisRepository.findVideoHeatRank(20);
        List<VideoSentimentDto> sentiments = analysisRepository.findVideoSentiments(100);
        List<DanmakuTimelineDto> timeline = analysisRepository.findDanmakuTimeline(null);
        List<UpPerformanceDto> upPerformance = analysisRepository.findUpPerformance(10);
        List<TaskDto> tasks = new ArrayList<>();

        heatRank.stream().findFirst().ifPresent(video -> tasks.add(task(
                "auto-review-top-video-" + video.bvid(),
                "优先复盘热度榜首",
                "高优先级",
                "warning",
                "触发规则：热度榜排名第 1。" + video.title() + " 当前位于热度榜首，建议进入复盘页拆解互动、弹幕和评论结构，沉淀可复用的选题方法。",
                video.bvid(),
                10
        )));

        sentiments.stream()
                .filter(item -> item.negativeRatio() >= 0.25)
                .max(Comparator.comparingDouble(VideoSentimentDto::negativeRatio))
                .ifPresent(video -> tasks.add(task(
                        "auto-negative-risk-" + video.bvid(),
                        "处理负面评论风险",
                        "风险",
                        "danger",
                        "触发规则：负向占比 >= 25%。" + video.title() + " 负向占比 " + formatPercent(video.negativeRatio()) + "，需要优先查看高赞负面评论并补充运营回应。",
                        video.bvid(),
                        20
                )));

        timeline.stream()
                .max(Comparator.comparingLong(DanmakuTimelineDto::danmakuCount))
                .ifPresent(hotspot -> tasks.add(task(
                        "auto-danmaku-hotspot-" + hotspot.bvid(),
                        "制作弹幕高能切片",
                        "可执行",
                        "primary",
                        "触发规则：弹幕时间轴峰值最高。" + hotspot.title() + " 在 " + formatVideoTime(hotspot.timeBucket()) + " 附近出现弹幕峰值，建议回看前后 15 秒制作切片。",
                        hotspot.bvid(),
                        30
                )));

        upPerformance.stream().findFirst().ifPresent(up -> tasks.add(task(
                "auto-up-sample-" + sanitizeTaskId(up.upName()),
                "沉淀高表现UP主样本",
                "增长",
                "success",
                "触发规则：UP主平均热度排名第 1。" + up.upName() + " 平均热度最高，可作为选题、标题和合作判断的样本池。",
                null,
                40
        )));

        sentiments.stream()
                .filter(item -> item.totalCount() > 0 && item.positiveRatio() >= 0.6)
                .max(Comparator.comparingDouble(VideoSentimentDto::positiveRatio))
                .ifPresent(video -> tasks.add(task(
                        "auto-positive-sample-" + video.bvid(),
                        "沉淀高口碑视频样本",
                        "增长",
                        "success",
                        "触发规则：评论样本数 > 0 且正向占比 >= 60%。" + video.title() + " 正向占比 " + formatPercent(video.positiveRatio()) + "，建议提炼评论区认可点用于后续选题复用。",
                        video.bvid(),
                        50
                )));

        return tasks;
    }

    private static TaskDto task(String taskId, String title, String level, String type, String text, String bvid, int sortNo) {
        return new TaskDto(taskId, title, level, type, text, bvid, "auto", sortNo, "todo", null, LocalDateTime.now(), LocalDateTime.now());
    }

    private static String formatPercent(double value) {
        return String.format("%.1f%%", value * 100);
    }

    private static String formatVideoTime(int seconds) {
        return seconds / 60 + ":" + String.format("%02d", seconds % 60);
    }

    private static String sanitizeTaskId(String value) {
        return value == null ? "unknown" : value.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", "-");
    }
}
