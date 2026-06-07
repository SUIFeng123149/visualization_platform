USE bilibili_analysis;

INSERT INTO ads_video_heat_rank
(bvid, title, up_name, category, view_count, like_count, coin_count, favorite_count, reply_count, danmaku_count, heat_score, rank_no)
VALUES
('BV_PLACEHOLDER_001', 'TODO_VIDEO_TITLE', 'TODO_UP_NAME', 'TODO_CATEGORY', 0, 0, 0, 0, 0, 0, 0.0, 1);

INSERT INTO ads_video_sentiment
(bvid, title, avg_sentiment, positive_count, neutral_count, negative_count, total_count, positive_ratio, negative_ratio)
VALUES
('BV_PLACEHOLDER_001', 'TODO_VIDEO_TITLE', 0.0, 0, 0, 0, 0, 0.0, 0.0);

INSERT INTO ads_sentiment_by_date
(stat_date, comment_count, danmaku_count, avg_sentiment, negative_ratio)
VALUES
('2026-01-01', 0, 0, 0.0, 0.0);

INSERT INTO ads_danmaku_timeline
(bvid, time_bucket, danmaku_count, avg_sentiment, top_words)
VALUES
('BV_PLACEHOLDER_001', 0, 0, 0.0, 'TODO_KEYWORD');

INSERT INTO ads_keyword_top
(dimension_type, dimension_value, word, word_count, rank_no)
VALUES
('global', 'all', 'TODO_KEYWORD', 0, 1);

INSERT INTO ads_up_performance
(up_name, video_count, avg_view_count, avg_heat_score, avg_sentiment, total_like_count)
VALUES
('TODO_UP_NAME', 0, 0.0, 0.0, 0.0, 0);

INSERT INTO dwd_comment_clean
(bvid, rpid, user_mid, user_name, text, like_count, crawled_at, source_type, raw_content, clean_content, content_length, clean_label, clean_score, is_valid_text, clean_reason, created_at)
VALUES
('BV_PLACEHOLDER_001', 'RP_PLACEHOLDER_001', 'MID_PLACEHOLDER_001', 'TODO_USER_NAME', 'TODO_RAW_COMMENT', 0, '2026-01-01 00:00:00', 'comment', 'TODO_RAW_COMMENT', 'TODO_CLEAN_COMMENT', 0, 'valid', 0.0, 1, NULL, '2026-01-01 00:00:00');

INSERT INTO dws_text_analysis_detail
(source_type, source_id, bvid, clean_content, tokens, keywords, sentiment_score, sentiment_label, model_version, created_at)
VALUES
('comment', 'RP_PLACEHOLDER_001', 'BV_PLACEHOLDER_001', 'TODO_CLEAN_COMMENT', 'TODO_TOKEN', 'TODO_KEYWORD', 0.0, 'negative', 'TODO_MODEL_VERSION', '2026-01-01 00:00:00');

INSERT INTO ops_task
(task_id, title, level, type, text, bvid, source, sort_no, is_active)
VALUES
('review-top-video', '优先复盘热度榜首', '高优先级', 'warning', '进入当前热度榜首视频复盘页，拆解互动、弹幕和评论结构，沉淀可复用的选题与标题方法。', 'BV_PLACEHOLDER_001', 'seed', 10, 1),
('handle-negative-comments', '处理负面评论风险', '风险', 'danger', '优先查看高赞负面评论，补充置顶解释、评论区回复或二次剪辑说明，降低舆情扩散。', 'BV_PLACEHOLDER_001', 'seed', 20, 1),
('clip-danmaku-hotspot', '制作弹幕高能切片', '可执行', 'primary', '根据弹幕时间轴峰值回看前后 15 秒，制作短切片或复盘该段内容设计。', 'BV_PLACEHOLDER_001', 'seed', 30, 1),
('build-creator-sample', '沉淀高表现 UP 样本', '增长', 'success', '将高热度、高互动率、正向情感较好的视频归入样本池，用于后续选题和合作判断。', NULL, 'seed', 40, 1);
