package com.bililens.analytics.collector.repository;

import com.bililens.analytics.collector.dto.CollectorTaskDto;
import com.bililens.analytics.collector.dto.CollectorTaskStatusUpdateRequest;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class CollectorTaskRepository {

    private final JdbcClient jdbcClient;

    public CollectorTaskRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @PostConstruct
    void ensureTable() {
        jdbcClient.sql("""
                        create table if not exists collector_task (
                            task_id varchar(64) primary key,
                            external_task_id varchar(128),
                            task_name varchar(120) not null,
                            natural_language text,
                            source_type varchar(64) not null,
                            collect_mode varchar(64) not null,
                            status varchar(32) not null,
                            progress int not null,
                            priority varchar(16) not null,
                            message varchar(1000),
                            row_count bigint,
                            raw_hdfs_path varchar(500),
                            clean_hdfs_path varchar(500),
                            batch_id varchar(128),
                            result_tables_json text,
                            params_json text not null,
                            callback_url varchar(500),
                            worker_id varchar(128),
                            created_at timestamp not null,
                            updated_at timestamp not null,
                            dispatched_at timestamp,
                            started_at timestamp,
                            finished_at timestamp
                        )
                        """)
                .update();
    }

    public CollectorTaskDto create(CollectorTaskDto task) {
        jdbcClient.sql("""
                        insert into collector_task (
                            task_id, external_task_id, task_name, natural_language, source_type, collect_mode,
                            status, progress, priority, message, row_count, raw_hdfs_path, clean_hdfs_path,
                            batch_id, result_tables_json, params_json, callback_url, worker_id,
                            created_at, updated_at, dispatched_at, started_at, finished_at
                        ) values (
                            :taskId, :externalTaskId, :taskName, :naturalLanguage, :sourceType, :collectMode,
                            :status, :progress, :priority, :message, :rowCount, :rawHdfsPath, :cleanHdfsPath,
                            :batchId, :resultTablesJson, :paramsJson, :callbackUrl, null,
                            :createdAt, :updatedAt, :dispatchedAt, :startedAt, :finishedAt
                        )
                        """)
                .param("taskId", task.taskId())
                .param("externalTaskId", task.externalTaskId())
                .param("taskName", task.taskName())
                .param("naturalLanguage", task.naturalLanguage())
                .param("sourceType", task.sourceType())
                .param("collectMode", task.collectMode())
                .param("status", task.status())
                .param("progress", task.progress())
                .param("priority", task.priority())
                .param("message", task.message())
                .param("rowCount", task.rowCount())
                .param("rawHdfsPath", task.rawHdfsPath())
                .param("cleanHdfsPath", task.cleanHdfsPath())
                .param("batchId", task.batchId())
                .param("resultTablesJson", String.join(",", task.resultTables()))
                .param("paramsJson", task.paramsJson())
                .param("callbackUrl", task.callbackUrl())
                .param("createdAt", task.createdAt())
                .param("updatedAt", task.updatedAt())
                .param("dispatchedAt", task.dispatchedAt())
                .param("startedAt", task.startedAt())
                .param("finishedAt", task.finishedAt())
                .update();
        return findById(task.taskId()).orElse(task);
    }

    public List<CollectorTaskDto> findRecent() {
        return jdbcClient.sql("""
                        select *
                        from collector_task
                        order by created_at desc
                        limit 50
                        """)
                .query((rs, rowNum) -> toDto(rs))
                .list();
    }

    public Optional<CollectorTaskDto> findById(String taskId) {
        return jdbcClient.sql("select * from collector_task where task_id = :taskId")
                .param("taskId", taskId)
                .query((rs, rowNum) -> toDto(rs))
                .optional();
    }

    public List<CollectorTaskDto> pullPending(String workerId, int limit) {
        List<String> taskIds = jdbcClient.sql("""
                        select task_id
                        from collector_task
                        where status = 'pending'
                        order by
                            case priority when 'urgent' then 1 when 'high' then 2 when 'normal' then 3 else 4 end,
                            created_at
                        limit :limit
                        """)
                .param("limit", limit)
                .query(String.class)
                .list();

        LocalDateTime now = LocalDateTime.now();
        for (String taskId : taskIds) {
            jdbcClient.sql("""
                            update collector_task
                            set status = 'dispatched',
                                progress = 5,
                                worker_id = :workerId,
                                message = '采集端已领取任务',
                                dispatched_at = :now,
                                updated_at = :now
                            where task_id = :taskId and status = 'pending'
                            """)
                    .param("workerId", workerId)
                    .param("now", now)
                    .param("taskId", taskId)
                    .update();
        }

        if (taskIds.isEmpty()) {
            return List.of();
        }
        return jdbcClient.sql("""
                        select *
                        from collector_task
                        where task_id in (:taskIds)
                        order by dispatched_at desc
                        """)
                .param("taskIds", taskIds)
                .query((rs, rowNum) -> toDto(rs))
                .list();
    }

    public CollectorTaskDto updateStatus(String taskId, CollectorTaskStatusUpdateRequest request) {
        CollectorTaskDto current = findById(taskId).orElseThrow(() -> new IllegalArgumentException("采集任务不存在"));
        String nextStatus = request.status() == null ? current.status() : request.status();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startedAt = current.startedAt();
        LocalDateTime finishedAt = current.finishedAt();
        if ("running".equals(nextStatus) && startedAt == null) {
            startedAt = now;
        }
        if (List.of("success", "failed", "cancelled").contains(nextStatus) && finishedAt == null) {
            finishedAt = now;
        }

        jdbcClient.sql("""
                        update collector_task
                        set external_task_id = :externalTaskId,
                            status = :status,
                            progress = :progress,
                            message = :message,
                            row_count = :rowCount,
                            raw_hdfs_path = :rawHdfsPath,
                            clean_hdfs_path = :cleanHdfsPath,
                            batch_id = :batchId,
                            result_tables_json = :resultTablesJson,
                            updated_at = :updatedAt,
                            started_at = :startedAt,
                            finished_at = :finishedAt
                        where task_id = :taskId
                        """)
                .param("externalTaskId", firstText(request.externalTaskId(), current.externalTaskId()))
                .param("status", nextStatus)
                .param("progress", request.progress() == null ? current.progress() : request.progress())
                .param("message", firstText(request.message(), current.message()))
                .param("rowCount", request.rowCount() == null ? current.rowCount() : request.rowCount())
                .param("rawHdfsPath", firstText(request.rawHdfsPath(), current.rawHdfsPath()))
                .param("cleanHdfsPath", firstText(request.cleanHdfsPath(), current.cleanHdfsPath()))
                .param("batchId", firstText(request.batchId(), current.batchId()))
                .param("resultTablesJson", request.resultTables() == null ? String.join(",", current.resultTables()) : String.join(",", request.resultTables()))
                .param("updatedAt", now)
                .param("startedAt", startedAt)
                .param("finishedAt", finishedAt)
                .param("taskId", taskId)
                .update();

        return findById(taskId).orElseThrow(() -> new IllegalArgumentException("采集任务不存在"));
    }

    public CollectorTaskDto cancel(String taskId) {
        CollectorTaskStatusUpdateRequest request = new CollectorTaskStatusUpdateRequest(
                "cancelled",
                100,
                null,
                "任务已由可视化平台取消",
                null,
                null,
                null,
                null,
                null
        );
        return updateStatus(taskId, request);
    }

    private CollectorTaskDto toDto(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new CollectorTaskDto(
                rs.getString("task_id"),
                rs.getString("external_task_id"),
                rs.getString("task_name"),
                rs.getString("natural_language"),
                rs.getString("source_type"),
                rs.getString("collect_mode"),
                rs.getString("status"),
                rs.getInt("progress"),
                rs.getString("priority"),
                rs.getString("message"),
                getLong(rs, "row_count"),
                rs.getString("raw_hdfs_path"),
                rs.getString("clean_hdfs_path"),
                rs.getString("batch_id"),
                splitCsv(rs.getString("result_tables_json")),
                rs.getString("params_json"),
                rs.getString("callback_url"),
                toLocalDateTime(rs.getTimestamp("created_at")),
                toLocalDateTime(rs.getTimestamp("updated_at")),
                toLocalDateTime(rs.getTimestamp("dispatched_at")),
                toLocalDateTime(rs.getTimestamp("started_at")),
                toLocalDateTime(rs.getTimestamp("finished_at"))
        );
    }

    private Long getLong(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private static String firstText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static List<String> splitCsv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
