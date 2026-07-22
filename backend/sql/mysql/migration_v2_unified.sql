-- 通用视频网站模型 v2。可在现有 bilibili_analysis 数据库中重复执行。
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
  INDEX idx_metric_platform_time (source_connector, captured_at),
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

INSERT INTO metric_dictionary (metric_key, display_name, unit, scope, definition, comparable)
VALUES
  ('view_count', '播放量', 'count', 'content', '平台返回的累计播放次数', 0),
  ('like_count', '点赞量', 'count', 'content', '用户表达喜欢的累计次数', 0),
  ('comment_count', '评论量', 'count', 'content', '评论、回复或剧评的累计次数', 0),
  ('share_count', '分享量', 'count', 'content', '平台记录的内容分享次数', 0),
  ('favorite_count', '收藏量', 'count', 'content', '收藏、追更或稍后看的累计次数', 0),
  ('danmaku_count', '弹幕量', 'count', 'content', '带视频内时间点互动的累计次数', 0),
  ('coin_count', '投币量', 'count', 'platform', '平台特有的投币或打赏次数', 0),
  ('completion_rate', '完播率', 'ratio', 'content', '内容播放完成比例', 1),
  ('rating', '评分', 'score', 'content', '平台提供的用户评分或口碑分', 0),
  ('interaction_rate', '互动率', 'ratio', 'content', '点赞、评论、分享等互动总量除以播放量', 1),
  ('platform_heat_score', '平台热度', 'score', 'platform', '平台内或平台策略计算的热度分', 0),
  ('normalized_heat_score', '归一化热度', 'percentile', 'cross_platform', '同平台、同内容类型、同时间窗口内的百分位热度', 1)
ON DUPLICATE KEY UPDATE display_name = VALUES(display_name), unit = VALUES(unit), scope = VALUES(scope), definition = VALUES(definition), comparable = VALUES(comparable);

-- MySQL 5.7 does not support ADD COLUMN/CREATE INDEX IF NOT EXISTS.
-- Use INFORMATION_SCHEMA so this additive migration remains repeatable.
SET @schema_name = DATABASE();

SET @sql = IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'dim_platform' AND COLUMN_NAME = 'connector_name') = 0,
  'ALTER TABLE dim_platform ADD COLUMN connector_name VARCHAR(128) NOT NULL DEFAULT '''' AFTER display_name',
  'SELECT 1'
);
PREPARE migration_statement FROM @sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

INSERT INTO dim_platform (platform_code, display_name, connector_name, capabilities_json)
VALUES
  ('bilibili', '哔哩哔哩', 'bilibili-export-v1', JSON_OBJECT('comments', true, 'replies', true, 'danmaku', true, 'reviews', false, 'series', false, 'completion_rate', false, 'creator_metrics', true, 'official_heat', false)),
  ('douyin', '抖音', 'douyin-approved-export-v1', JSON_OBJECT('comments', true, 'replies', true, 'danmaku', true, 'reviews', false, 'series', false, 'completion_rate', true, 'creator_metrics', true, 'official_heat', false)),
  ('iqiyi', '爱奇艺', 'iqiyi-approved-export-v1', JSON_OBJECT('comments', true, 'replies', false, 'danmaku', true, 'reviews', true, 'series', true, 'completion_rate', true, 'creator_metrics', false, 'official_heat', true)),
  ('youku', '优酷', 'youku-approved-export-v1', JSON_OBJECT('comments', true, 'replies', false, 'danmaku', true, 'reviews', true, 'series', true, 'completion_rate', true, 'creator_metrics', false, 'official_heat', true))
ON DUPLICATE KEY UPDATE display_name = VALUES(display_name), connector_name = VALUES(connector_name), capabilities_json = VALUES(capabilities_json), enabled = 1;

SET @sql = IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'ops_task' AND COLUMN_NAME = 'content_id') = 0,
  'ALTER TABLE ops_task ADD COLUMN content_id BIGINT NULL',
  'SELECT 1'
);
PREPARE migration_statement FROM @sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @sql = IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'ops_task' AND COLUMN_NAME = 'platform_code') = 0,
  'ALTER TABLE ops_task ADD COLUMN platform_code VARCHAR(32) NULL',
  'SELECT 1'
);
PREPARE migration_statement FROM @sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @sql = IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'ops_task' AND COLUMN_NAME = 'external_content_id') = 0,
  'ALTER TABLE ops_task ADD COLUMN external_content_id VARCHAR(255) NULL',
  'SELECT 1'
);
PREPARE migration_statement FROM @sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @sql = IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
   WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'ops_task' AND INDEX_NAME = 'idx_ops_task_content') = 0,
  'CREATE INDEX idx_ops_task_content ON ops_task (content_id)',
  'SELECT 1'
);
PREPARE migration_statement FROM @sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;
