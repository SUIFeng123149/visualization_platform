insert into ads_video_heat_rank
(bvid, title, up_name, category, view_count, like_count, coin_count, favorite_count, reply_count, danmaku_count, heat_score, rank_no)
values
('BV003', 'B站弹幕情感分析案例', '数据小助手', '知识', 188000, 13200, 4200, 7800, 3600, 9200, 85920.0, 1),
('BV001', 'Python大数据课设从零到一', '数据小助手', '知识', 93000, 10100, 3100, 5200, 2200, 6100, 67120.0, 2),
('BV002', 'ECharts可视化看板搭建指南', '前端研究所', '科技', 126000, 7800, 2100, 4300, 1800, 4800, 60300.0, 3),
('BV004', 'UP主增长数据复盘方法', '运营观察员', '知识', 72000, 4200, 900, 2200, 1200, 2600, 34980.0, 4);

insert into dim_platform (platform_code, display_name, connector_name, capabilities_json, enabled) values
('bilibili', '哔哩哔哩', 'bilibili-export-v1', '{"comments":true,"replies":true,"reviews":false,"danmaku":true}', 1),
('douyin', '抖音', 'douyin-approved-export-v1', '{"comments":true,"replies":true,"reviews":false,"danmaku":true}', 1),
('iqiyi', '爱奇艺', 'iqiyi-approved-export-v1', '{"comments":true,"replies":false,"reviews":true,"danmaku":true,"series":true}', 1),
('youku', '优酷', 'youku-approved-export-v1', '{"comments":true,"replies":false,"reviews":true,"danmaku":true}', 1);

insert into dim_account (account_id, platform_code, external_account_id, display_name, account_type) values
(1, 'bilibili', 'UP001', '数据小助手', 'creator'),
(2, 'iqiyi', 'ORG001', '星河影视', 'publisher'),
(3, 'youku', 'ORG002', '云帆剧场', 'publisher');

insert into dim_content (content_id, platform_code, external_content_id, content_type, account_id, title, category, published_at) values
(1, 'bilibili', 'BV003', 'video', 1, 'B站弹幕情感分析案例', '知识', '2026-05-20 10:00:00'),
(2, 'douyin', 'DY001', 'short_video', null, '短视频案例', '科技', '2026-05-21 10:00:00'),
(3, 'iqiyi', 'IQ_SERIES_1', 'series', 2, '星河计划', '电视剧', '2026-05-01 20:00:00'),
(4, 'iqiyi', 'IQ_EP_1', 'episode', 2, '星河计划 第一集', '电视剧', '2026-05-22 20:00:00'),
(5, 'youku', 'YK_EP_1', 'episode', 3, '云端追光 第一集', '电视剧', '2026-05-23 20:00:00');

update dim_content set parent_content_id = 3 where content_id = 4;

insert into fact_content_metric_snapshot
(content_id, captured_at, view_count, like_count, comment_count, share_count, favorite_count, danmaku_count, coin_count, extra_metrics, platform_heat_score, normalized_heat_score) values
(1, '2026-05-26 10:00:00', 188000, 13200, 3600, 1000, 7800, 9200, 4200, '{"completion_events": 510}', 85920, 0.92),
(2, '2026-05-26 10:00:00', 250000, 18000, 2400, 3200, 5000, null, null, '{"completion_events": 730}', 91000, 0.88),
(3, '2026-05-26 10:00:00', 800000, null, 12000, 900, 21000, null, null, null, 93000, 0.95),
(4, '2026-05-26 10:00:00', 320000, 6000, 4200, 320, 8000, null, null, '{"completion_events": "425"}', 72000, 0.81),
(5, '2026-05-26 10:00:00', 280000, 5200, 3600, 280, 7200, null, null, '{"completion_events": 365}', 68000, 0.78);

insert into metric_dictionary (metric_key, display_name, unit, scope, definition, comparable) values
('view_count', '播放量', 'count', 'content', '平台返回的累计播放次数', 0),
('interaction_rate', '互动率', 'ratio', 'content', '互动总量除以播放量', 1),
('normalized_heat_score', '归一化热度', 'percentile', 'cross_platform', '同类内容的相对热度', 1);
insert into metric_dictionary (metric_key, display_name, unit, scope, definition, comparable) values
('completion_events', 'Completion events', 'count', 'content', 'Connector-provided completion event count.', 0);

insert into fact_interaction
(interaction_id, content_id, platform_code, external_interaction_id, interaction_type, external_user_id, user_name, text, like_count, video_time_seconds, occurred_at, captured_at, batch_id, raw_attributes) values
(1, 1, 'bilibili', 'BILI_C1', 'comment', 'U1', '用户甲', '讲得很清楚，案例实用', 120, null, '2026-05-25 09:00:00', '2026-05-25 10:00:00', 'mock-batch', '{}'),
(2, 1, 'bilibili', 'BILI_D1', 'danmaku', 'U2', '用户乙', '这里高能', 8, 65, '2026-05-25 09:01:00', '2026-05-25 10:00:00', 'mock-batch', '{}'),
(3, 1, 'bilibili', 'BILI_D2', 'danmaku', 'U3', '用户丙', '模型有点难', 3, 78, '2026-05-25 09:02:00', '2026-05-25 10:00:00', 'mock-batch', '{}'),
(4, 2, 'douyin', 'DY_C1', 'comment', 'DU1', '短视频用户', '节奏很快但信息有用', 88, null, '2026-05-26 11:00:00', '2026-05-26 12:00:00', 'mock-batch', '{}'),
(5, 4, 'iqiyi', 'IQ_R1', 'review', 'IU1', '追剧用户', '第一集剧情完整', 36, null, '2026-05-26 20:00:00', '2026-05-26 21:00:00', 'mock-batch', '{}'),
(6, 1, 'bilibili', 'BILI_C2', 'comment', 'U4', '复盘用户', '案例有用，但情感模型解释不够清楚', 66, null, '2026-05-26 09:30:00', '2026-05-26 10:00:00', 'mock-batch', '{}'),
(7, 2, 'douyin', 'DY_C2', 'comment', 'DU2', '短视频观众', '信息不少，但字幕太快看不清', 142, null, '2026-05-26 11:30:00', '2026-05-26 12:00:00', 'mock-batch', '{}'),
(8, 2, 'douyin', 'DY_R1', 'reply', 'DU3', '互动用户', '回复说明后可以理解', 23, null, '2026-05-26 11:40:00', '2026-05-26 12:00:00', 'mock-batch', '{}'),
(9, 4, 'iqiyi', 'IQ_R2', 'review', 'IU2', '剧评用户', '节奏拖沓，人物动机不够合理', 98, null, '2026-05-25 20:00:00', '2026-05-25 21:00:00', 'mock-batch', '{}'),
(10, 5, 'youku', 'YK_R1', 'review', 'YU1', '追剧观众', '镜头质感很好，主角表现自然', 75, null, '2026-05-24 20:00:00', '2026-05-24 21:00:00', 'mock-batch', '{}'),
(11, 5, 'youku', 'YK_C1', 'comment', 'YU2', '评论用户', '更新太慢，剧情推进也有点拖', 109, null, '2026-05-26 20:30:00', '2026-05-26 21:00:00', 'mock-batch', '{}');

insert into fact_text_analysis
(analysis_id, interaction_id, sentiment_score, sentiment_label, tokens, keywords, model_version, created_at) values
(1, 1, 0.86, 'positive', '清楚,案例,实用', '清楚,案例', 'mock-v1', '2026-05-25 10:05:00'),
(2, 2, 0.91, 'positive', '高能', '高能', 'mock-v1', '2026-05-25 10:05:00'),
(3, 3, 0.28, 'negative', '模型,难', '模型,难度', 'mock-v1', '2026-05-25 10:05:00'),
(4, 4, 0.68, 'positive', '节奏,信息,有用', '节奏,信息', 'mock-v1', '2026-05-26 12:05:00'),
(5, 5, 0.76, 'positive', '剧情,完整', '剧情,完整', 'mock-v1', '2026-05-26 21:05:00'),
(6, 6, 0.27, 'negative', '模型,解释,清楚', '模型,解释', 'mock-v1', '2026-05-26 10:05:00'),
(7, 7, 0.19, 'negative', '信息,字幕,太快', '信息,字幕', 'mock-v1', '2026-05-26 12:05:00'),
(8, 8, 0.55, 'neutral', '回复,理解', '回复,理解', 'mock-v1', '2026-05-26 12:05:00'),
(9, 9, 0.16, 'negative', '节奏,拖沓,动机', '节奏,剧情', 'mock-v1', '2026-05-25 21:05:00'),
(10, 10, 0.88, 'positive', '镜头,质感,自然', '镜头,质感', 'mock-v1', '2026-05-24 21:05:00'),
(11, 11, 0.21, 'negative', '更新,剧情,拖', '更新,剧情', 'mock-v1', '2026-05-26 21:05:00');

insert into ads_video_sentiment
(bvid, title, avg_sentiment, positive_count, neutral_count, negative_count, total_count, positive_ratio, negative_ratio)
values
('BV001', 'Python大数据课设从零到一', 0.516, 2, 2, 1, 5, 0.4, 0.2),
('BV002', 'ECharts可视化看板搭建指南', 0.642, 4, 2, 1, 7, 0.571, 0.143),
('BV003', 'B站弹幕情感分析案例', 0.703, 7, 2, 1, 10, 0.7, 0.1),
('BV004', 'UP主增长数据复盘方法', 0.438, 2, 3, 2, 7, 0.286, 0.286);

insert into ads_sentiment_by_date
(stat_date, comment_count, danmaku_count, avg_sentiment, negative_ratio)
values
('2026-05-20', 3, 3, 0.52, 0.25),
('2026-05-21', 4, 5, 0.57, 0.18),
('2026-05-22', 6, 8, 0.61, 0.14),
('2026-05-23', 8, 11, 0.66, 0.12),
('2026-05-24', 7, 10, 0.59, 0.2),
('2026-05-25', 9, 13, 0.68, 0.1),
('2026-05-26', 11, 16, 0.71, 0.08);

insert into ads_danmaku_timeline
(bvid, time_bucket, danmaku_count, avg_sentiment, top_words)
values
('BV001', 30, 3, 0.45, '开头 课程'),
('BV001', 90, 8, 0.62, '截图 配置'),
('BV001', 180, 5, 0.38, '报错 环境'),
('BV001', 270, 12, 0.74, '跑通 项目'),
('BV001', 360, 7, 0.58, '总结 收藏'),
('BV002', 120, 9, 0.67, '图表 大屏 ECharts'),
('BV003', 60, 6, 0.66, '弹幕 高能'),
('BV003', 120, 15, 0.79, '情感分析 好用'),
('BV003', 240, 10, 0.51, '模型 数据');

insert into ads_keyword_top
(dimension_type, dimension_value, word, word_count, rank_no)
values
('global', 'all', '情感分析', 3, 1),
('global', 'all', '弹幕', 3, 2),
('global', 'all', 'ECharts', 2, 3),
('global', 'all', 'Python', 2, 4),
('global', 'all', '可视化', 2, 5),
('bvid', 'BV001', '配置', 2, 1),
('bvid', 'BV001', '截图', 2, 2),
('bvid', 'BV003', '高能', 2, 1);

insert into ads_up_performance
(up_name, video_count, avg_view_count, avg_heat_score, avg_sentiment, total_like_count)
values
('数据小助手', 2, 140500.0, 76500.0, 0.546, 23300),
('前端研究所', 1, 126000.0, 60300.0, 0.642, 7800),
('运营观察员', 1, 72000.0, 34980.0, 0.438, 4200);

insert into dwd_comment_clean
(bvid, rpid, user_mid, user_name, text, like_count, crawled_at, source_type, raw_content, clean_content, content_length, clean_label, clean_score, is_valid_text, clean_reason, created_at)
values
('BV001', 'R001', 'U001', '课代表一号', '环境配置这里讲得有点快，还是报错。', 128, '2026-05-20 10:15:00', 'comment', '环境配置这里讲得有点快，还是报错。', '环境配置讲得快 报错', 16, 'valid', 0.92, 1, null, '2026-05-20 10:16:00'),
('BV001', 'R002', 'U002', '数据练习生', '跟着做跑通了，感谢。', 96, '2026-05-20 11:20:00', 'comment', '跟着做跑通了，感谢。', '跟着做 跑通 感谢', 10, 'valid', 0.96, 1, null, '2026-05-20 11:21:00'),
('BV004', 'R003', 'U003', '运营同学', '这个复盘方法不够具体。', 64, '2026-05-24 12:10:00', 'comment', '这个复盘方法不够具体。', '复盘方法 不够具体', 11, 'valid', 0.88, 1, null, '2026-05-24 12:11:00');

insert into dws_text_analysis_detail
(source_type, source_id, bvid, clean_content, tokens, keywords, sentiment_score, sentiment_label, model_version, created_at)
values
('comment', 'R001', 'BV001', '环境配置讲得快 报错', '环境,配置,报错', '配置,报错', 0.22, 'negative', 'sentiment-v1', '2026-05-20 10:17:00'),
('comment', 'R002', 'BV001', '跟着做 跑通 感谢', '跑通,感谢', '跑通,感谢', 0.82, 'positive', 'sentiment-v1', '2026-05-20 11:22:00'),
('comment', 'R003', 'BV004', '复盘方法 不够具体', '复盘,具体', '复盘', 0.31, 'negative', 'sentiment-v1', '2026-05-24 12:12:00');
