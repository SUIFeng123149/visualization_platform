# BiliLens 数据可视化平台

BiliLens 是一个面向 B 站内容分析场景的数据可视化平台。项目围绕视频热度、评论情感、弹幕高峰、主题关键词、UP 主画像、运营任务、报表历史和 AI 辅助分析，提供从数据汇总到运营复盘的一体化看板。

当前项目包含三部分：

- `backend`：Spring Boot REST API，负责读取 MySQL 分析表、生成任务、记录报表、代理 Dify AI 调用。
- `frontend`：Vue 3 + Vite 数据看板，负责图表展示、筛选、复盘、导出和 AI 交互。
- `bilibili_data_pipeline`：数据处理与导入相关脚本和文档，产出 DWD/DWS/ADS 层数据。

## 功能概览

- 数据总览：展示视频热度排行、播放/互动趋势、情感结构、弹幕高峰、关键词和数据质量。
- 视频分析：查看视频样本池、热度对比、互动率、正向占比和可复盘视频。
- 视频复盘：单视频详情页展示基础指标、复盘评分、情感结构、弹幕切片、关键词、相似视频和负面评论样本。
- 弹幕分析：定位视频高能时间点，辅助二创切片和内容复盘。
- 评论洞察：分析评论情感趋势、正负向占比和高赞负面评论。
- UP 主画像：对比创作者热度、播放、互动和口碑表现。
- 任务中心：根据热度、情感、弹幕和 UP 主表现自动生成运营任务，并保存任务状态。
- 数据监控：查看关键数据表行数、最近更新时间和健康状态。
- 报表中心：记录报表导出历史，支持按模块导出 Excel。
- 规则管理：维护异常检测规则，例如高热度、负向情绪、互动率和弹幕高峰阈值。
- AI 助手：接入 Dify，支持页面解读、视频复盘、异常诊断、报表摘要和平台问数。

## 技术栈

| 模块 | 技术 |
| --- | --- |
| 前端 | Vue 3, Vite, Element Plus, ECharts, xlsx |
| 后端 | Java 21, Spring Boot 4, Spring JDBC, Validation, Actuator |
| 数据库 | MySQL 8，测试环境使用 H2 |
| AI | Dify Chat API，后端代理调用 |
| 构建 | npm, Maven |

## 目录结构

```text
visualization_platform/
├── backend/                  # Spring Boot 后端服务
│   ├── src/main/java/         # Controller, Service, Repository, DTO
│   ├── src/main/resources/    # application.yml
│   ├── src/test/              # H2 测试库和集成测试
│   └── sql/mysql/             # MySQL 建表脚本和样例数据
├── frontend/                 # Vue 前端看板
│   ├── src/api/               # API 请求封装
│   ├── src/components/        # 布局、图表、表格、AI 组件
│   ├── src/composables/       # 数据加载和图表逻辑
│   ├── src/views/             # 页面模块
│   ├── src/styles/            # 全局样式
│   └── vite.config.js         # Vite 代理和构建配置
├── bilibili_data_pipeline/    # 数据处理、导入脚本和交付文档
├── bilibili/                  # 采集相关子模块
├── bilibili_ai_dify/          # Dify/AI 相关材料
└── README.md
```

## 环境要求

- Node.js 18+
- npm 9+
- JDK 21
- Maven 3.9+
- MySQL 8+

## 数据库初始化

默认数据库名为 `bilibili_analysis`。

```powershell
cd backend
mysql -u root -p < sql/mysql/schema.sql
mysql -u root -p bilibili_analysis < sql/mysql/placeholder-data.sql
```

`placeholder-data.sql` 只用于演示。真实数据应由数据处理流程写入 DWD/DWS/ADS 表。

### 核心数据表

| 表名 | 作用 |
| --- | --- |
| `dwd_comment_clean` | 评论清洗明细，支持负面评论样本 |
| `dwd_danmaku_clean` | 弹幕清洗明细，支持弹幕时间分析 |
| `dws_text_analysis_detail` | 文本分析明细，包含分词、关键词、情感分和标签 |
| `ads_video_heat_rank` | 视频热度排行，驱动视频榜单和总览 |
| `ads_video_sentiment` | 视频情感统计，驱动正向占比和情感图表 |
| `ads_sentiment_by_date` | 按日期聚合的情感趋势 |
| `ads_danmaku_timeline` | 视频弹幕时间轴聚合 |
| `ads_keyword_top` | 全局/视频维度关键词 TopN |
| `ads_up_performance` | UP 主表现聚合 |
| `ops_task` | 自动生成或人工录入的运营任务 |
| `ops_task_status` | 任务状态持久化 |
| `ops_report_history` | 报表导出历史 |
| `ops_anomaly_rule` | 异常检测规则 |

## 后端启动

默认配置在 [backend/src/main/resources/application.yml](backend/src/main/resources/application.yml)。

当前默认连接：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/bilibili_analysis?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: 123456
```

启动：

```powershell
cd backend
mvn "-Dmaven.repo.local=.m2/repository" spring-boot:run
```

健康检查：

```text
GET http://localhost:8080/actuator/health
```

运行测试：

```powershell
cd backend
mvn "-Dmaven.repo.local=.m2/repository" test
```

## 前端启动

开发环境下，Vite 已将 `/api` 和 `/actuator` 代理到 `http://localhost:8080`。

```powershell
cd frontend
npm install
npm run dev
```

访问：

```text
http://localhost:5173
```

生产构建：

```powershell
cd frontend
npm run build
```

本地预览：

```powershell
cd frontend
npm run preview
```

## Dify AI 助手配置

AI 助手由后端代理调用 Dify，前端不会暴露 API Key。

后端配置项：

```yaml
analytics:
  dify:
    base-url: ${DIFY_BASE_URL:}
    api-key: ${DIFY_API_KEY:}
    user: ${DIFY_USER:bililens-user}
```

推荐使用环境变量：

```powershell
$env:DIFY_BASE_URL="https://api.dify.ai/v1"
$env:DIFY_API_KEY="app-xxxx"
$env:DIFY_USER="bililens-user"
```

如果未配置 Dify，`/api/ai/chat` 会返回本地兜底提示，不影响其它页面使用。

推荐 Dify 应用提示词：

```text
你是 BiliLens 数据可视化平台的 AI 数据分析助手。
请基于用户问题和传入的页面上下文进行分析。
如果上下文不足，必须明确说明缺少哪些数据，不要编造指标。
回答优先包含：结论、原因、建议、需要补充的数据。
```

## 常用 API

所有业务响应统一包装为：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 分析接口

| 接口 | 说明 |
| --- | --- |
| `GET /api/analysis/videos/heat-rank?limit=100` | 视频热度排行 |
| `GET /api/analysis/videos/historical-samples?limit=30` | 历史文本样本视频 |
| `GET /api/analysis/videos/sentiment?limit=100` | 视频情感统计 |
| `GET /api/analysis/videos/sentiment/by-bvids?bvids=BV1,BV2` | 按 BVID 查询情感 |
| `GET /api/analysis/videos/{bvid}/detail` | 单视频复盘详情 |
| `GET /api/analysis/sentiment/trend?startDate=2026-05-01&endDate=2026-06-08` | 情感趋势 |
| `GET /api/analysis/danmaku/timeline?bvid=BVxxx` | 弹幕时间轴 |
| `GET /api/analysis/keywords?dimensionType=global&dimensionValue=all&limit=30` | 关键词 TopN |
| `GET /api/analysis/ups/performance?limit=10` | UP 主表现 |
| `GET /api/analysis/comments/negative?bvid=BVxxx&limit=20` | 负面评论样本 |

### 运营和平台接口

| 接口 | 说明 |
| --- | --- |
| `GET /api/tasks` | 查询任务列表 |
| `POST /api/tasks/refresh` | 基于当前数据重新生成任务 |
| `PUT /api/tasks/statuses/{taskId}` | 更新任务状态 |
| `GET /api/platform/data-sources/status` | 数据源状态 |
| `GET /api/platform/reports` | 报表历史 |
| `POST /api/platform/reports` | 记录报表导出 |
| `GET /api/platform/anomaly-rules` | 异常规则 |
| `POST /api/ai/chat` | AI 助手问答 |

## 数据口径

- 热度分：来自 `ads_video_heat_rank.heat_score`，综合播放、互动、评论和弹幕等指标。
- 互动总量：点赞、投币、收藏、评论、弹幕数量的合计。
- 互动率：互动总量 / 播放量。
- 正向占比：`positive_count / total_count`，无情感样本时不计算。
- 负向占比：`negative_count / total_count`，用于舆情风险判断。
- 弹幕高峰：来自 `ads_danmaku_timeline` 中同一时间桶的弹幕数量。
- 关键词：优先使用 `ads_keyword_top`，单视频缺失时可从 `dws_text_analysis_detail.keywords` 回退生成。

## 已知约束

- 热度榜接口当前最大 `limit` 为 100，因此前端默认最多加载 100 条热度样本。
- 若导入数据中时间字段为 `2026-06-02T11:16:29+00:00` 这类 ISO 字符串，后端负面评论查询已做兼容；更推荐入库时统一为 MySQL `DATETIME` 格式。
- `ads_keyword_top` 是聚合表，如果某个视频没有该表记录，视频详情会尝试从 DWS 文本分析明细中回退生成关键词。
- 任务中心的数据由 `/api/tasks/refresh` 基于当前数据库分析结果生成并写入 `ops_task`，状态写入 `ops_task_status`。
- 报表中心只记录导出历史和元信息，Excel 文件由浏览器端即时生成下载。

## 常见问题

### 前端只显示 100 条视频

这是接口限制，不是数据库缺数据。前端默认请求 `limit=100`，后端 `heat-rank` 接口也限制最大 100。

### 某些视频没有正向占比

通常是 `ads_video_sentiment` 中没有该 BVID 的情感聚合记录，或者该视频没有评论/文本分析样本。

### 视频详情没有关键词

优先检查：

```sql
select *
from ads_keyword_top
where dimension_type = 'bvid'
  and dimension_value = '目标BVID';
```

如果聚合表为空，再检查：

```sql
select count(*)
from dws_text_analysis_detail
where bvid = '目标BVID'
  and keywords is not null
  and keywords <> '';
```

### 任务中心刷新后没有任务

检查 `ads_video_heat_rank`、`ads_video_sentiment`、`ads_danmaku_timeline`、`ads_up_performance` 是否有数据。任务生成依赖这些分析结果。

### AI 助手只返回配置提示

说明后端没有配置 Dify：

```powershell
$env:DIFY_BASE_URL="https://api.dify.ai/v1"
$env:DIFY_API_KEY="app-xxxx"
```

配置后重启后端。

## 验证命令

```powershell
# 后端测试
cd backend
mvn "-Dmaven.repo.local=.m2/repository" test

# 前端构建
cd frontend
npm run build

# 验证单视频复盘接口
Invoke-RestMethod -Uri "http://localhost:8080/api/analysis/videos/BV1s3LA64ErQ/detail" -Method Get
```

## 安全说明

- 不要把真实数据库密码、Dify API Key 或其它密钥提交到 Git。
- 生产环境建议通过环境变量或密钥管理系统配置敏感信息。
- `placeholder-data.sql` 只用于演示，不代表真实业务数据。
