-- Copy all unified v2 application data from the existing local database into
-- a new video_analytics database. Run schema_v2.sql against video_analytics first.
-- This script intentionally excludes legacy ADS/DWD tables and the deprecated BVID field.

SET FOREIGN_KEY_CHECKS = 0;

INSERT INTO video_analytics.dim_platform
  (platform_code, display_name, connector_name, capabilities_json, enabled, created_at, updated_at)
SELECT platform_code, display_name, connector_name, capabilities_json, enabled, created_at, updated_at
FROM bilibili_analysis.dim_platform
ON DUPLICATE KEY UPDATE
  display_name=VALUES(display_name), connector_name=VALUES(connector_name),
  capabilities_json=VALUES(capabilities_json), enabled=VALUES(enabled), updated_at=VALUES(updated_at);

INSERT INTO video_analytics.metric_dictionary
  (metric_key, display_name, unit, scope, definition, comparable, created_at)
SELECT metric_key, display_name, unit, scope, definition, comparable, created_at
FROM bilibili_analysis.metric_dictionary
ON DUPLICATE KEY UPDATE
  display_name=VALUES(display_name), unit=VALUES(unit), scope=VALUES(scope),
  definition=VALUES(definition), comparable=VALUES(comparable);

INSERT INTO video_analytics.dim_account
  (account_id, platform_code, external_account_id, display_name, account_type, raw_attributes, created_at, updated_at)
SELECT account_id, platform_code, external_account_id, display_name, account_type, raw_attributes, created_at, updated_at
FROM bilibili_analysis.dim_account
ON DUPLICATE KEY UPDATE
  platform_code=VALUES(platform_code), external_account_id=VALUES(external_account_id),
  display_name=VALUES(display_name), account_type=VALUES(account_type), raw_attributes=VALUES(raw_attributes),
  updated_at=VALUES(updated_at);

INSERT INTO video_analytics.dim_content
  (content_id, platform_code, external_content_id, content_type, parent_content_id, account_id, title, category,
   published_at, duration_seconds, cover_url, description, raw_attributes, created_at, updated_at)
SELECT content_id, platform_code, external_content_id, content_type, parent_content_id, account_id, title, category,
       published_at, duration_seconds, cover_url, description, raw_attributes, created_at, updated_at
FROM bilibili_analysis.dim_content
ON DUPLICATE KEY UPDATE
  platform_code=VALUES(platform_code), external_content_id=VALUES(external_content_id), content_type=VALUES(content_type),
  parent_content_id=VALUES(parent_content_id), account_id=VALUES(account_id), title=VALUES(title), category=VALUES(category),
  published_at=VALUES(published_at), duration_seconds=VALUES(duration_seconds), cover_url=VALUES(cover_url),
  description=VALUES(description), raw_attributes=VALUES(raw_attributes), updated_at=VALUES(updated_at);

INSERT INTO video_analytics.fact_content_metric_snapshot
  (snapshot_id, content_id, captured_at, batch_id, source_connector, view_count, like_count, comment_count,
   share_count, favorite_count, danmaku_count, coin_count, completion_rate, rating, platform_heat_score,
   normalized_heat_score, extra_metrics, raw_payload_ref)
SELECT snapshot_id, content_id, captured_at, batch_id, source_connector, view_count, like_count, comment_count,
       share_count, favorite_count, danmaku_count, coin_count, completion_rate, rating, platform_heat_score,
       normalized_heat_score, extra_metrics, raw_payload_ref
FROM bilibili_analysis.fact_content_metric_snapshot
ON DUPLICATE KEY UPDATE
  content_id=VALUES(content_id), captured_at=VALUES(captured_at), batch_id=VALUES(batch_id),
  view_count=VALUES(view_count), like_count=VALUES(like_count), comment_count=VALUES(comment_count),
  share_count=VALUES(share_count), favorite_count=VALUES(favorite_count), danmaku_count=VALUES(danmaku_count),
  coin_count=VALUES(coin_count), completion_rate=VALUES(completion_rate), rating=VALUES(rating),
  platform_heat_score=VALUES(platform_heat_score), normalized_heat_score=VALUES(normalized_heat_score),
  extra_metrics=VALUES(extra_metrics), raw_payload_ref=VALUES(raw_payload_ref);

INSERT INTO video_analytics.fact_interaction
  (interaction_id, content_id, platform_code, external_interaction_id, interaction_type, external_user_id,
   user_name, text, like_count, video_time_seconds, occurred_at, captured_at, batch_id, raw_attributes)
SELECT interaction_id, content_id, platform_code, external_interaction_id, interaction_type, external_user_id,
       user_name, text, like_count, video_time_seconds, occurred_at, captured_at, batch_id, raw_attributes
FROM bilibili_analysis.fact_interaction
ON DUPLICATE KEY UPDATE
  content_id=VALUES(content_id), platform_code=VALUES(platform_code), external_interaction_id=VALUES(external_interaction_id),
  interaction_type=VALUES(interaction_type), external_user_id=VALUES(external_user_id), user_name=VALUES(user_name),
  text=VALUES(text), like_count=VALUES(like_count), video_time_seconds=VALUES(video_time_seconds),
  occurred_at=VALUES(occurred_at), captured_at=VALUES(captured_at), batch_id=VALUES(batch_id), raw_attributes=VALUES(raw_attributes);

INSERT INTO video_analytics.fact_text_analysis
  (analysis_id, interaction_id, sentiment_score, sentiment_label, tokens, keywords, model_version, created_at)
SELECT analysis_id, interaction_id, sentiment_score, sentiment_label, tokens, keywords, model_version, created_at
FROM bilibili_analysis.fact_text_analysis
ON DUPLICATE KEY UPDATE
  interaction_id=VALUES(interaction_id), sentiment_score=VALUES(sentiment_score), sentiment_label=VALUES(sentiment_label),
  tokens=VALUES(tokens), keywords=VALUES(keywords), model_version=VALUES(model_version), created_at=VALUES(created_at);

INSERT INTO video_analytics.ops_task
  (task_id, title, level, type, text, content_id, platform_code, external_content_id, source, sort_no,
   is_active, created_at, updated_at)
SELECT task_id, title, level, type, text, content_id, platform_code, external_content_id, source, sort_no,
       is_active, created_at, updated_at
FROM bilibili_analysis.ops_task
ON DUPLICATE KEY UPDATE
  title=VALUES(title), level=VALUES(level), type=VALUES(type), text=VALUES(text), content_id=VALUES(content_id),
  platform_code=VALUES(platform_code), external_content_id=VALUES(external_content_id), source=VALUES(source),
  sort_no=VALUES(sort_no), is_active=VALUES(is_active), updated_at=VALUES(updated_at);

INSERT INTO video_analytics.ops_task_status (task_id, status, updated_at, created_at)
SELECT task_id, status, updated_at, created_at FROM bilibili_analysis.ops_task_status
ON DUPLICATE KEY UPDATE status=VALUES(status), updated_at=VALUES(updated_at);

INSERT INTO video_analytics.ops_report_history
  (id, report_name, report_type, status, row_count, file_name, remark, created_at)
SELECT id, report_name, report_type, status, row_count, file_name, remark, created_at
FROM bilibili_analysis.ops_report_history
ON DUPLICATE KEY UPDATE
  report_name=VALUES(report_name), report_type=VALUES(report_type), status=VALUES(status),
  row_count=VALUES(row_count), file_name=VALUES(file_name), remark=VALUES(remark), created_at=VALUES(created_at);

INSERT INTO video_analytics.ops_anomaly_rule
  (id, rule_key, name, metric, operator, threshold_value, level, enabled, description, created_at, updated_at)
SELECT id, rule_key, name, metric, operator, threshold_value, level, enabled, description, created_at, updated_at
FROM bilibili_analysis.ops_anomaly_rule
ON DUPLICATE KEY UPDATE
  name=VALUES(name), metric=VALUES(metric), operator=VALUES(operator), threshold_value=VALUES(threshold_value),
  level=VALUES(level), enabled=VALUES(enabled), description=VALUES(description), updated_at=VALUES(updated_at);

SET FOREIGN_KEY_CHECKS = 1;
