insert into ads_video_heat_rank
(bvid, title, up_name, category, view_count, like_count, coin_count, favorite_count, reply_count, danmaku_count, heat_score, rank_no)
values
('BV003', 'B站弹幕情感分析案例', '数据小助手', '知识', 188000, 13200, 4200, 7800, 3600, 9200, 85920.0, 1),
('BV001', 'Python大数据课设从零到一', '数据小助手', '知识', 93000, 10100, 3100, 5200, 2200, 6100, 67120.0, 2),
('BV002', 'ECharts可视化看板搭建指南', '前端研究所', '科技', 126000, 7800, 2100, 4300, 1800, 4800, 60300.0, 3),
('BV004', 'UP主增长数据复盘方法', '运营观察员', '知识', 72000, 4200, 900, 2200, 1200, 2600, 34980.0, 4);

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
