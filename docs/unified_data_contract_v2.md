# 通用视频网站数据协议 v2

本协议用于抖音、哔哩哔哩、爱奇艺、优酷等平台。平台连接器负责把原始响应转换为本协议；分析层和 API 层不得依赖平台专有字段。

## 标识与层级

- `platform_code`: `bilibili`、`douyin`、`iqiyi`、`youku`。
- `external_content_id`: 平台侧内容 ID，例如 BV 号、视频 ID、剧集 ID。
- `content_id`: 平台内部生成的稳定 BIGINT，业务查询优先使用它。
- `content_type`: `short_video`、`video`、`episode`、`movie`、`series`。
- `parent_content_id`: 集归属于剧集、单集归属于专辑时使用。
- `external_account_id`: 平台侧创作者、频道或发行方 ID。

## 标准内容字段

`title`、`category`、`published_at`、`duration_seconds`、`account_id`、`cover_url`、`description`、`raw_attributes`。

## 标准指标

连接器应尽量映射以下字段，平台不支持时使用 NULL，不得填充为 0：

`view_count`、`like_count`、`comment_count`、`share_count`、`favorite_count`、`danmaku_count`、`coin_count`、`completion_rate`、`rating`。

其他指标写入 `extra_metrics` JSON，并在 `metric_dictionary` 中声明名称、单位和口径。

所有指标都必须带 `captured_at`。同一内容的多次采集写入快照，不覆盖历史值。

## 互动协议

互动类型为 `comment`、`danmaku`、`review`、`reply`。弹幕是平台能力，不是所有内容的必有字段。互动文本分析统一输出情感、关键词和模型版本。

## 热度口径

- `platform_heat_score`: 平台或平台内策略计算的分数，只用于平台内比较。
- `normalized_heat_score`: 在平台、内容类型和发布时间窗口内归一化后的百分位分数，用于跨平台比较。
- 任何排行都必须返回 `metric_definition`，说明计算口径和时间范围。

## 平台能力

能力键包括 `comments`、`danmaku`、`reviews`、`series`、`completion_rate`、`creator_metrics`、`official_heat`。前端根据能力隐藏或降级模块，后端不得把缺失能力当成零值。

## 批次与可追溯性

每条标准记录携带 `batch_id`、`source_connector`、`captured_at` 和可选 `raw_payload_ref`。原始数据保留在 ODS，标准化失败的记录进入 dead-letter 输出并包含错误原因。
