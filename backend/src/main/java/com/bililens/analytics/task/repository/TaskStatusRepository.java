package com.bililens.analytics.task.repository;

import com.bililens.analytics.task.dto.TaskDto;
import com.bililens.analytics.task.dto.TaskStatusDto;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class TaskStatusRepository {

    private final JdbcClient jdbcClient;

    public TaskStatusRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<TaskStatusDto> findAll() {
        return jdbcClient.sql("""
                        select task_id, status, updated_at, created_at
                        from ops_task_status
                        order by updated_at desc
                        """)
                .query((rs, rowNum) -> new TaskStatusDto(
                        rs.getString("task_id"),
                        rs.getString("status"),
                        toLocalDateTime(rs.getTimestamp("updated_at")),
                        toLocalDateTime(rs.getTimestamp("created_at"))
                ))
                .list();
    }

    public List<TaskDto> findActiveTasks() {
        return jdbcClient.sql("""
                        select t.task_id, t.title, t.level, t.type, t.text, t.content_id, t.platform_code,
                               t.external_content_id, t.bvid, t.source, t.sort_no,
                               coalesce(s.status, 'todo') as status,
                               s.updated_at as status_updated_at,
                               t.created_at, t.updated_at
                        from ops_task t
                        left join ops_task_status s on t.task_id = s.task_id
                        where t.is_active = 1
                        order by t.sort_no, t.created_at, t.task_id
                        """)
                .query((rs, rowNum) -> new TaskDto(
                        rs.getString("task_id"),
                        rs.getString("title"),
                        rs.getString("level"),
                        rs.getString("type"),
                        rs.getString("text"),
                        rs.getObject("content_id", Long.class),
                        rs.getString("platform_code"),
                        rs.getString("external_content_id"),
                        rs.getString("bvid"),
                        rs.getString("source"),
                        rs.getInt("sort_no"),
                        rs.getString("status"),
                        toLocalDateTime(rs.getTimestamp("status_updated_at")),
                        toLocalDateTime(rs.getTimestamp("created_at")),
                        toLocalDateTime(rs.getTimestamp("updated_at"))
                ))
                .list();
    }

    public boolean existsTask(String taskId) {
        Long count = jdbcClient.sql("select count(*) from ops_task where task_id = :taskId")
                .param("taskId", taskId)
                .query(Long.class)
                .single();
        return count != null && count > 0;
    }

    public void deactivateAutoTasks() {
        jdbcClient.sql("""
                        update ops_task
                        set is_active = 0, updated_at = current_timestamp
                        where source = 'auto'
                        """)
                .update();
    }

    public void upsertTask(TaskDto task) {
        int updated = jdbcClient.sql("""
                        update ops_task
                        set title = :title,
                            level = :level,
                            type = :type,
                            text = :text,
                            content_id = :contentId,
                            platform_code = :platformCode,
                            external_content_id = :externalContentId,
                            bvid = :bvid,
                            source = :source,
                            sort_no = :sortNo,
                            is_active = 1,
                            updated_at = current_timestamp
                        where task_id = :taskId
                        """)
                .param("title", task.title())
                .param("level", task.level())
                .param("type", task.type())
                .param("text", task.text())
                .param("contentId", task.contentId())
                .param("platformCode", task.platformCode())
                .param("externalContentId", task.externalContentId())
                .param("bvid", task.bvid())
                .param("source", task.source())
                .param("sortNo", task.sortNo())
                .param("taskId", task.taskId())
                .update();

        if (updated == 0) {
            jdbcClient.sql("""
                            insert into ops_task
                            (task_id, title, level, type, text, content_id, platform_code, external_content_id, bvid, source, sort_no, is_active, created_at, updated_at)
                            values
                            (:taskId, :title, :level, :type, :text, :contentId, :platformCode, :externalContentId, :bvid, :source, :sortNo, 1, current_timestamp, current_timestamp)
                            """)
                    .param("taskId", task.taskId())
                    .param("title", task.title())
                    .param("level", task.level())
                    .param("type", task.type())
                    .param("text", task.text())
                    .param("contentId", task.contentId())
                    .param("platformCode", task.platformCode())
                    .param("externalContentId", task.externalContentId())
                    .param("bvid", task.bvid())
                    .param("source", task.source())
                    .param("sortNo", task.sortNo())
                    .update();
        }
    }

    public TaskStatusDto upsert(String taskId, String status) {
        int updated = jdbcClient.sql("""
                        update ops_task_status
                        set status = :status, updated_at = current_timestamp
                        where task_id = :taskId
                        """)
                .param("status", status)
                .param("taskId", taskId)
                .update();

        if (updated == 0) {
            jdbcClient.sql("""
                            insert into ops_task_status (task_id, status, updated_at, created_at)
                            values (:taskId, :status, current_timestamp, current_timestamp)
                            """)
                    .param("taskId", taskId)
                    .param("status", status)
                    .update();
        }

        return jdbcClient.sql("""
                        select task_id, status, updated_at, created_at
                        from ops_task_status
                        where task_id = :taskId
                        """)
                .param("taskId", taskId)
                .query((rs, rowNum) -> new TaskStatusDto(
                        rs.getString("task_id"),
                        rs.getString("status"),
                        toLocalDateTime(rs.getTimestamp("updated_at")),
                        toLocalDateTime(rs.getTimestamp("created_at"))
                ))
                .single();
    }

    private static LocalDateTime toLocalDateTime(java.sql.Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
