CREATE DATABASE IF NOT EXISTS bilibili_analysis
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE bilibili_analysis;

DROP TABLE IF EXISTS dws_text_analysis_detail;
DROP TABLE IF EXISTS dwd_comment_clean;
DROP TABLE IF EXISTS dwd_danmaku_clean;
DROP TABLE IF EXISTS ads_up_performance;
DROP TABLE IF EXISTS ads_keyword_top;
DROP TABLE IF EXISTS ads_danmaku_timeline;
DROP TABLE IF EXISTS ads_sentiment_by_date;
DROP TABLE IF EXISTS ads_video_sentiment;
DROP TABLE IF EXISTS ads_video_heat_rank;
DROP TABLE IF EXISTS ops_task_status;
DROP TABLE IF EXISTS ops_task;

CREATE TABLE dwd_comment_clean (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  bvid VARCHAR(32),
  rpid VARCHAR(64),
  user_mid VARCHAR(64),
  user_name VARCHAR(128),
  text TEXT,
  like_count BIGINT,
  crawled_at DATETIME,
  source_type VARCHAR(16),
  raw_content TEXT,
  clean_content TEXT,
  content_length INT,
  clean_label VARCHAR(32),
  clean_score DOUBLE,
  is_valid_text TINYINT,
  clean_reason VARCHAR(64),
  created_at DATETIME,
  UNIQUE KEY uk_rpid (rpid),
  INDEX idx_bvid (bvid),
  INDEX idx_valid_label (is_valid_text, clean_label)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE dwd_danmaku_clean (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  bvid VARCHAR(32),
  cid VARCHAR(64),
  video_time_seconds DOUBLE,
  text TEXT,
  crawled_at DATETIME,
  danmaku_id VARCHAR(128),
  source_type VARCHAR(16),
  raw_content TEXT,
  clean_content TEXT,
  content_length INT,
  clean_label VARCHAR(32),
  clean_score DOUBLE,
  is_valid_text TINYINT,
  clean_reason VARCHAR(64),
  created_at DATETIME,
  UNIQUE KEY uk_danmaku_id (danmaku_id),
  INDEX idx_bvid_time (bvid, video_time_seconds)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE dws_text_analysis_detail (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  source_type VARCHAR(16),
  source_id VARCHAR(128),
  bvid VARCHAR(32),
  clean_content TEXT,
  tokens TEXT,
  keywords TEXT,
  sentiment_score DOUBLE,
  sentiment_label VARCHAR(16),
  model_version VARCHAR(64),
  created_at DATETIME,
  INDEX idx_bvid (bvid),
  INDEX idx_source (source_type, source_id),
  INDEX idx_sentiment (sentiment_label)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ads_video_heat_rank (
  bvid VARCHAR(32) PRIMARY KEY,
  title VARCHAR(255),
  up_name VARCHAR(100),
  category VARCHAR(64),
  view_count BIGINT,
  like_count BIGINT,
  coin_count BIGINT,
  favorite_count BIGINT,
  reply_count BIGINT,
  danmaku_count BIGINT,
  heat_score DOUBLE,
  rank_no INT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ads_video_sentiment (
  bvid VARCHAR(32) PRIMARY KEY,
  title VARCHAR(255),
  avg_sentiment DOUBLE,
  positive_count BIGINT,
  neutral_count BIGINT,
  negative_count BIGINT,
  total_count BIGINT,
  positive_ratio DOUBLE,
  negative_ratio DOUBLE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ads_sentiment_by_date (
  stat_date DATE PRIMARY KEY,
  comment_count BIGINT,
  danmaku_count BIGINT,
  avg_sentiment DOUBLE,
  negative_ratio DOUBLE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ads_danmaku_timeline (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  bvid VARCHAR(32),
  time_bucket INT,
  danmaku_count BIGINT,
  avg_sentiment DOUBLE,
  top_words TEXT,
  INDEX idx_bvid_bucket (bvid, time_bucket)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ads_keyword_top (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  dimension_type VARCHAR(32),
  dimension_value VARCHAR(128),
  word VARCHAR(64),
  word_count BIGINT,
  rank_no INT,
  INDEX idx_dimension (dimension_type, dimension_value, rank_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ads_up_performance (
  up_name VARCHAR(100) PRIMARY KEY,
  video_count BIGINT,
  avg_view_count DOUBLE,
  avg_heat_score DOUBLE,
  avg_sentiment DOUBLE,
  total_like_count BIGINT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ops_task_status (
  task_id VARCHAR(255) PRIMARY KEY,
  status VARCHAR(32) NOT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_status (status),
  INDEX idx_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ops_task (
  task_id VARCHAR(255) PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  level VARCHAR(32) NOT NULL,
  type VARCHAR(32) NOT NULL,
  text TEXT NOT NULL,
  bvid VARCHAR(32),
  source VARCHAR(64) NOT NULL DEFAULT 'system',
  sort_no INT NOT NULL DEFAULT 100,
  is_active TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_active_sort (is_active, sort_no),
  INDEX idx_bvid (bvid),
  INDEX idx_source (source)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
