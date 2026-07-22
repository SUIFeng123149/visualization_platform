-- Clean, platform-neutral MySQL schema for the v2 application.
-- Run this file in a new database. It does not create legacy Bilibili ADS/DWD tables.

CREATE TABLE IF NOT EXISTS dim_platform (
  platform_code VARCHAR(32) PRIMARY KEY,
  display_name VARCHAR(64) NOT NULL,
  connector_name VARCHAR(128) NOT NULL,
  capabilities_json JSON NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dim_account (
  account_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  platform_code VARCHAR(32) NOT NULL,
  external_account_id VARCHAR(128) NOT NULL,
  display_name VARCHAR(255) NOT NULL,
  account_type VARCHAR(32) NOT NULL DEFAULT 'creator',
  raw_attributes JSON,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_account_platform_external (platform_code, external_account_id),
  INDEX idx_account_platform_name (platform_code, display_name),
  CONSTRAINT fk_account_platform FOREIGN KEY (platform_code) REFERENCES dim_platform(platform_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dim_content (
  content_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  platform_code VARCHAR(32) NOT NULL,
  external_content_id VARCHAR(255) NOT NULL,
  content_type VARCHAR(32) NOT NULL DEFAULT 'video',
  parent_content_id BIGINT NULL,
  account_id BIGINT NULL,
  title VARCHAR(500) NOT NULL,
  category VARCHAR(128),
  published_at DATETIME NULL,
  duration_seconds INT NULL,
  cover_url VARCHAR(1000),
  description TEXT,
  raw_attributes JSON,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_content_platform_external (platform_code, external_content_id),
  INDEX idx_content_parent (parent_content_id),
  INDEX idx_content_account (account_id),
  INDEX idx_content_type_published (content_type, published_at),
  CONSTRAINT fk_content_platform FOREIGN KEY (platform_code) REFERENCES dim_platform(platform_code),
  CONSTRAINT fk_content_parent FOREIGN KEY (parent_content_id) REFERENCES dim_content(content_id),
  CONSTRAINT fk_content_account FOREIGN KEY (account_id) REFERENCES dim_account(account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS fact_content_metric_snapshot (
  snapshot_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  content_id BIGINT NOT NULL,
  captured_at DATETIME NOT NULL,
  batch_id VARCHAR(128) NOT NULL,
  source_connector VARCHAR(128) NOT NULL,
  view_count BIGINT NULL,
  like_count BIGINT NULL,
  comment_count BIGINT NULL,
  share_count BIGINT NULL,
  favorite_count BIGINT NULL,
  danmaku_count BIGINT NULL,
  coin_count BIGINT NULL,
  completion_rate DOUBLE NULL,
  rating DOUBLE NULL,
  platform_heat_score DOUBLE NULL,
  normalized_heat_score DOUBLE NULL,
  extra_metrics JSON,
  raw_payload_ref VARCHAR(1000),
  UNIQUE KEY uk_metric_content_capture (content_id, captured_at, source_connector),
  INDEX idx_metric_connector_time (source_connector, captured_at),
  CONSTRAINT fk_metric_content FOREIGN KEY (content_id) REFERENCES dim_content(content_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS fact_interaction (
  interaction_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  content_id BIGINT NOT NULL,
  platform_code VARCHAR(32) NOT NULL,
  external_interaction_id VARCHAR(255),
  interaction_type VARCHAR(32) NOT NULL,
  external_user_id VARCHAR(255),
  user_name VARCHAR(255),
  text TEXT,
  like_count BIGINT NULL,
  video_time_seconds DOUBLE NULL,
  occurred_at DATETIME NULL,
  captured_at DATETIME NOT NULL,
  batch_id VARCHAR(128) NOT NULL,
  raw_attributes JSON,
  UNIQUE KEY uk_interaction_external (platform_code, interaction_type, external_interaction_id),
  INDEX idx_interaction_content_type (content_id, interaction_type, captured_at),
  CONSTRAINT fk_interaction_content FOREIGN KEY (content_id) REFERENCES dim_content(content_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS fact_text_analysis (
  analysis_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  interaction_id BIGINT NOT NULL,
  sentiment_score DOUBLE,
  sentiment_label VARCHAR(16),
  tokens TEXT,
  keywords TEXT,
  model_version VARCHAR(64) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_text_analysis_model (interaction_id, model_version),
  CONSTRAINT fk_text_analysis_interaction FOREIGN KEY (interaction_id) REFERENCES fact_interaction(interaction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS metric_dictionary (
  metric_key VARCHAR(64) PRIMARY KEY,
  display_name VARCHAR(128) NOT NULL,
  unit VARCHAR(32),
  scope VARCHAR(32) NOT NULL DEFAULT 'platform',
  definition TEXT,
  comparable TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ops_task_status (
  task_id VARCHAR(255) PRIMARY KEY,
  status VARCHAR(32) NOT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_status (status),
  INDEX idx_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ops_task (
  task_id VARCHAR(255) PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  level VARCHAR(32) NOT NULL,
  type VARCHAR(32) NOT NULL,
  text TEXT NOT NULL,
  content_id BIGINT NULL,
  platform_code VARCHAR(32) NULL,
  external_content_id VARCHAR(255) NULL,
  source VARCHAR(64) NOT NULL DEFAULT 'system',
  sort_no INT NOT NULL DEFAULT 100,
  is_active TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_ops_task_content (content_id),
  INDEX idx_ops_task_active_sort (is_active, sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ops_report_history (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  report_name VARCHAR(255) NOT NULL,
  report_type VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  row_count BIGINT NOT NULL DEFAULT 0,
  file_name VARCHAR(500),
  remark TEXT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_report_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ops_anomaly_rule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  rule_key VARCHAR(64) NOT NULL,
  name VARCHAR(128) NOT NULL,
  metric VARCHAR(64) NOT NULL,
  operator VARCHAR(16) NOT NULL,
  threshold_value DOUBLE NOT NULL,
  level VARCHAR(32) NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  description TEXT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_anomaly_rule_key (rule_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO metric_dictionary (metric_key, display_name, unit, scope, definition, comparable) VALUES
  ('view_count', '播放量', 'count', 'content', '平台返回的累计播放次数。', 0),
  ('like_count', '点赞量', 'count', 'content', '累计点赞或同类正向反馈次数。', 0),
  ('comment_count', '互动量', 'count', 'content', '平台统计的评论、回复或剧评数量。', 0),
  ('share_count', '分享量', 'count', 'content', '平台统计的内容分享次数。', 0),
  ('favorite_count', '收藏量', 'count', 'content', '收藏、关注或稍后观看次数。', 0),
  ('danmaku_count', '时序互动量', 'count', 'content', '与视频内时间点关联的互动数量。', 0),
  ('coin_count', '平台激励量', 'count', 'platform', '平台特有的投币、打赏或激励数量。', 0),
  ('completion_rate', '完播率', 'ratio', 'content', '完成播放的观众比例。', 1),
  ('rating', '评分', 'score', 'content', '平台提供的用户评分或口碑分数。', 0),
  ('interaction_rate', '互动率', 'ratio', 'content', '互动总量除以播放量。', 1),
  ('platform_heat_score', '平台热度', 'score', 'platform', '平台内或平台策略计算的热度分数。', 0),
  ('normalized_heat_score', '归一化热度', 'percentile', 'cross_platform', '可跨平台比较的归一化热度分数。', 1)
ON DUPLICATE KEY UPDATE display_name=VALUES(display_name), unit=VALUES(unit), scope=VALUES(scope), definition=VALUES(definition), comparable=VALUES(comparable);

INSERT INTO dim_platform (platform_code, display_name, connector_name, capabilities_json) VALUES
  ('bilibili', '哔哩哔哩', 'bilibili-export-v1', JSON_OBJECT('comments', true, 'replies', true, 'danmaku', true, 'reviews', false, 'series', false, 'completion_rate', false, 'creator_metrics', true, 'official_heat', false)),
  ('douyin', '抖音', 'douyin-approved-export-v1', JSON_OBJECT('comments', true, 'replies', true, 'danmaku', true, 'reviews', false, 'series', false, 'completion_rate', true, 'creator_metrics', true, 'official_heat', false)),
  ('iqiyi', '爱奇艺', 'iqiyi-approved-export-v1', JSON_OBJECT('comments', true, 'replies', false, 'danmaku', true, 'reviews', true, 'series', true, 'completion_rate', true, 'creator_metrics', false, 'official_heat', true)),
  ('youku', '优酷', 'youku-approved-export-v1', JSON_OBJECT('comments', true, 'replies', false, 'danmaku', true, 'reviews', true, 'series', true, 'completion_rate', true, 'creator_metrics', false, 'official_heat', true))
ON DUPLICATE KEY UPDATE display_name=VALUES(display_name), connector_name=VALUES(connector_name), capabilities_json=VALUES(capabilities_json), enabled=1;
