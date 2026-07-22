# BiliLens 数据可视化平台

BiliLens 是一个面向 B 站内容运营、舆情观察和视频复盘的数据可视化平台。平台把视频热度、评论情感、弹幕峰值、关键词、UP 主表现、数据质量、异常检测、运营任务和 AI 解读整合到同一套看板中，帮助运营人员从“发现数据变化”进一步落到“定位原因、复盘内容、生成动作”。

项目采用前后端分离架构：

- `frontend`：Vue 3 + Vite + Element Plus + ECharts，可视化看板和 AI 交互界面。
- `backend`：Spring Boot REST API，读取 MySQL 分析表，提供任务、报表、异常规则和 Dify AI 代理接口。
- `bilibili_data_pipeline`：数据采集、清洗、分析和导入链路，负责从 Hive/CSV 等来源产出 DWD/DWS/ADS 层数据。

## 功能概览

### 数据总览

- 展示总播放量、互动总量、平均情感、最高热度、正向占比等核心指标。
- 展示播放/互动/情感趋势、弹幕峰值散点、评论情感占比、UP 主能力雷达图。
- 提供数据质量提示、AI 解读当前总览、异常检测中心和运营动作建议。
- 支持按分区、时间周期筛选，并导出完整数据报表。

### 视频分析与视频复盘

- 视频列表支持关键词搜索、热度排序、互动率、正向占比和复盘入口。
- 视频详情页展示基础指标、复盘评分、评论情感、弹幕高峰、关键词、相似视频、负面评论样本和 AI 复盘建议。
- 支持从数据总览、弹幕分析、任务中心等页面跳转到具体视频复盘页。

### 弹幕分析

- 基于 `ads_danmaku_timeline` 定位弹幕集中爆发的时间片段。
- 用于识别视频高能段落、剪辑切片、开头留存点和用户共鸣点。

### 评论洞察

- 展示评论情感趋势、正中负向占比和高赞负面评论样本。
- 服务舆情监控、用户反馈归因和内容风险排查。

### UP 主画像

- 对比 UP 主视频数、平均播放、平均热度、平均情感、点赞总量等表现。
- 用于识别头部账号、潜力账号和合作优先级。

### 任务中心

- 根据热度、情感、互动、弹幕和 UP 主表现生成运营任务。
- 支持任务状态持久化，便于跟踪复盘动作是否落地。

### 数据监控

- 展示关键数据表的数据量、最近更新时间和健康状态。
- 用于确认图表数据是否可信，排查数据源缺失、同步失败和导入异常。

### 报表中心

- 记录报表导出历史。
- 前端通过 `xlsx` 生成 Excel 文件，后端保存导出元信息。

### 异常规则管理

- 管理异常检测规则，如高热度、负向情感、互动率异常、弹幕峰值异常等。
- 支持规则配置保存，作为异常检测中心的规则基础。

### AI 助手

- 后端代理调用 Dify，前端不直接暴露 API Key。
- 支持普通聊天接口 `/api/ai/chat` 和 SSE 流式聊天接口 `/api/ai/chat-stream`。
- AI 用于页面解读、视频复盘、异常诊断、报表摘要和平台问数。

## 技术栈

| 模块 | 技术 |
| --- | --- |
| 前端 | Vue 3, Vite, Vue Router, Element Plus, ECharts, markdown-it, xlsx |
| 后端 | Java 21, Spring Boot 4, Spring MVC, Spring JDBC, Validation, Actuator |
| 数据库 | MySQL 8；测试环境使用 H2 |
| AI | Dify Chat API；后端代理；支持 SSE 流式输出 |
| 数据处理 | Python, PySpark, Hive/CSV 输入, MySQL 导入 |
| 构建工具 | npm, Maven |

## 目录结构

```text
visualization_platform/
├─ backend/                       # Spring Boot 后端服务
│  ├─ src/main/java/               # Controller, Service, Repository, DTO
│  ├─ src/main/resources/          # application.yml
│  ├─ src/test/                    # H2 测试配置和集成测试
│  └─ sql/mysql/                   # MySQL 建表脚本和演示数据
├─ frontend/                      # Vue 前端看板
│  ├─ src/api/                     # API 请求封装
│  ├─ src/components/              # 图表、表格、AI、质量提示等组件
│  ├─ src/composables/             # 数据加载、指标计算、导出逻辑
│  ├─ src/router/                  # 页面路由
│  ├─ src/views/                   # 页面模块
│  └─ src/styles/                  # 全局样式
├─ bilibili_data_pipeline/         # 数据处理、分析、导入脚本和文档
├─ bilibili/                       # 采集与数据输入相关材料
├─ bilibili_ai_dify/               # AI/Dify 相关材料
├─ hrbust-dify-re/                 # Dify 本地部署/二开相关目录
└─ README.md
```

## 环境要求

- Node.js 18+
- npm 9+
- JDK 21
- Maven 3.9+
- MySQL 8+
- Python 3.9+，仅运行 `bilibili_data_pipeline` 时需要

## 快速启动

### 1. 初始化数据库

默认数据库名为 `bilibili_analysis`。先创建数据库，再执行建表和演示数据脚本：

```powershell
mysql -u root -p -e "create database if not exists bilibili_analysis default character set utf8mb4 collate utf8mb4_unicode_ci;"
mysql -u root -p bilibili_analysis < backend\sql\mysql\schema.sql
mysql -u root -p bilibili_analysis < backend\sql\mysql\migration_v2_unified.sql
mysql -u root -p bilibili_analysis < backend\sql\mysql\placeholder-data.sql
```

`migration_v2_unified.sql` 创建通用内容、指标快照和互动表，是默认跨平台界面的必需步骤。`placeholder-data.sql` 仅用于旧版 B 站界面演示；真实数据应由数据管道按 v2 契约生成并导入。

### 2. 启动后端

默认配置位于 [backend/src/main/resources/application.yml](backend/src/main/resources/application.yml)。当前默认端口为 `8080`，默认连接本地 MySQL：

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/bilibili_analysis?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: 123456
```

启动命令：

```powershell
cd backend
mvn "-Dmaven.repo.local=.m2/repository" spring-boot:run
```

健康检查：

```text
GET http://localhost:8080/actuator/health
```

### 3. 启动前端

开发环境下，Vite 会把 `/api` 和 `/actuator` 代理到 `http://localhost:8080`。

```powershell
cd frontend
npm install
npm run dev
```

访问地址：

```text
http://localhost:5173
```

### 4. 生产构建

```powershell
cd frontend
npm run build
npm run preview
```

## 前端页面路由

| 路由 | 页面 | 说明 |
| --- | --- | --- |
| `/overview` | 数据总览 | 核心指标、趋势图、数据质量、AI 解读、异常检测和运营建议 |
| `/video` | 视频分析 | 视频样本池、热度对比、复盘入口 |
| `/video/:bvid` | 视频复盘 | 单视频详情、情感、弹幕、关键词、负面评论和相似视频 |
| `/danmaku` | 弹幕分析 | 弹幕峰值时间轴和高能片段 |
| `/comment` | 评论洞察 | 评论情感趋势和负面样本 |
| `/creator` | UP 主画像 | 创作者表现对比 |
| `/task` | 任务中心 | 运营任务生成、状态更新 |
| `/data-sources` | 数据监控 | 数据源状态、表行数、更新时间 |
| `/reports` | 报表中心 | 导出历史记录 |
| `/anomaly-rules` | 规则管理 | 异常检测规则配置 |
| `/ai-assistant` | AI 助手 | 平台问数和流式 AI 对话 |

## 后端 API

所有业务接口统一返回：

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
| `GET /api/analysis/videos/sentiment/by-bvids?bvids=BV1,BV2` | 按 BVID 批量查询情感 |
| `GET /api/analysis/videos/{bvid}/detail` | 单视频复盘详情 |
| `GET /api/analysis/sentiment/trend?startDate=2026-05-01&endDate=2026-06-08` | 情感趋势 |
| `GET /api/analysis/danmaku/timeline?bvid=BVxxx` | 弹幕时间轴 |
| `GET /api/analysis/keywords?dimensionType=global&dimensionValue=all&limit=30` | 关键词 TopN |
| `GET /api/analysis/ups/performance?limit=10` | UP 主表现 |
| `GET /api/analysis/comments/negative?bvid=BVxxx&limit=20` | 负面评论样本 |
| `POST /api/analysis/cache/evict` | 清理后端分析缓存 |

### 运营与平台接口

| 接口 | 说明 |
| --- | --- |
| `GET /api/tasks` | 查询运营任务列表 |
| `GET /api/tasks/statuses` | 查询任务状态 |
| `POST /api/tasks/refresh` | 根据当前数据重新生成任务 |
| `PUT /api/tasks/statuses/{taskId}` | 更新任务状态 |
| `GET /api/platform/data-sources/status` | 数据源状态 |
| `GET /api/platform/reports` | 报表历史 |
| `POST /api/platform/reports` | 记录报表导出 |
| `GET /api/platform/anomaly-rules` | 查询异常规则 |
| `PUT /api/platform/anomaly-rules` | 保存异常规则 |

### AI 接口

| 接口 | 说明 |
| --- | --- |
| `POST /api/ai/chat` | 普通 AI 问答 |
| `POST /api/ai/chat-stream` | SSE 流式 AI 问答 |

AI 请求由后端代理到 Dify。前端 AI 页面和各页面 AI 解读组件会把当前页面上下文传给后端，后端再调用 Dify 应用。

## Dify 配置

后端 AI 配置位于 [backend/src/main/resources/application.yml](backend/src/main/resources/application.yml)。推荐使用下面这种环境变量形式，不要把真实 Dify API Key 写死在仓库中：

```yaml
analytics:
  dify:
    base-url: ${DIFY_BASE_URL:http://localhost/v1}
    api-key: ${DIFY_API_KEY:}
    user: ${DIFY_USER:bililens-user}
```

本地或生产环境建议通过环境变量覆盖：

```powershell
$env:DIFY_BASE_URL="https://api.dify.ai/v1"
$env:DIFY_API_KEY="app-xxxx"
$env:DIFY_USER="bililens-user"
```

如果 `application.yml` 中存在本地调试默认 Key，提交或部署前应删除默认值，改为只从环境变量读取。

推荐 Dify 应用提示词方向：

```text
你是 BiliLens 数据可视化平台的 AI 数据分析助手。
请严格基于用户问题和传入的页面上下文回答。
如果上下文缺少关键指标，必须说明缺少哪些数据，不要编造结论。
回答优先包含：结论、原因、建议、需要补充的数据。
格式要求：标题清晰，列表紧凑，冒号不要单独换行。
```

## 数据链路

平台展示层主要消费 MySQL 中的 DWD/DWS/ADS 表。推荐链路如下：

```text
Hive / CSV / API 原始数据
  -> bilibili_data_pipeline 数据准备
  -> 文本清洗、分词、情感分析、热度计算、弹幕聚合
  -> output/<dataset>_output/csv/
  -> import_to_mysql.py 导入 MySQL
  -> Spring Boot API
  -> Vue + ECharts 可视化看板
```

数据管道默认输入协议：

```text
exports/
  videos.csv
  comments.csv
  danmaku.csv
```

`videos.csv` 字段：

```csv
bvid,aid,title,up_mid,up_name,category,published_at,cid,view_count,danmaku_count,like_count,coin_count,favorite_count,share_count,crawled_at
```

`comments.csv` 字段：

```csv
bvid,rpid,user_mid,user_name,text,like_count,crawled_at
```

`danmaku.csv` 字段：

```csv
bvid,cid,video_time_seconds,text,crawled_at
```

## 核心数据表

| 表名 | 作用 |
| --- | --- |
| `dwd_comment_clean` | 评论清洗明细，支持负面评论样本 |
| `dwd_danmaku_clean` | 弹幕清洗明细，支持弹幕时间分析 |
| `dws_text_analysis_detail` | 文本分析明细，包含关键词、情感分和标签 |
| `ads_video_heat_rank` | 视频热度排行，驱动视频榜单和总览 |
| `ads_video_sentiment` | 视频情感统计，驱动正负向占比和情感图表 |
| `ads_sentiment_by_date` | 按日期聚合的情感趋势 |
| `ads_danmaku_timeline` | 视频弹幕时间轴聚合 |
| `ads_keyword_top` | 全局/视频维度关键词 TopN |
| `ads_up_performance` | UP 主表现聚合 |
| `ops_task` | 自动生成或人工录入的运营任务 |
| `ops_task_status` | 任务状态持久化 |
| `ops_report_history` | 报表导出历史 |
| `ops_anomaly_rule` | 异常检测规则 |

## 指标口径

- 热度分：来自 `ads_video_heat_rank.heat_score`，综合播放、点赞、投币、收藏、分享、评论、弹幕等指标。
- 互动总量：点赞、投币、收藏、分享、评论、弹幕数量的合计。
- 互动率：互动总量 / 播放量。
- 平均情感：来自 `ads_video_sentiment.avg_sentiment` 或文本分析聚合结果。
- 正向占比：`positive_count / total_count`，缺少情感样本时不计算。
- 负向占比：`negative_count / total_count`，用于舆情风险判断。
- 弹幕峰值：来自 `ads_danmaku_timeline` 中同一时间桶的弹幕数量。
- 关键词：优先使用 `ads_keyword_top`；单视频聚合缺失时可从 `dws_text_analysis_detail.keywords` 回退生成。

## 数据管道运行

进入数据处理目录：

```powershell
cd bilibili_data_pipeline
```

安装依赖：

```powershell
.\scripts\install_dependencies.ps1
```

使用示例数据运行分析：

```powershell
.\.venv\Scripts\python.exe src\analysis_pyspark.py --run --input-dir data\data_example
```

使用成员 B Hive 整理后的数据运行分析：

```powershell
.\.venv\Scripts\python.exe src\analysis_pyspark.py --run --input-dir data\b_hive_dataset --output-dir output\b_dataset_output
```

将分析结果导入 MySQL：

```powershell
.\.venv\Scripts\python.exe src\import_to_mysql.py --input-dir output\b_dataset_output\csv
```

更多数据管道细节见 [bilibili_data_pipeline/README.md](bilibili_data_pipeline/README.md) 和 `bilibili_data_pipeline/docs/`。

## 常用验证命令

后端测试：

```powershell
cd backend
mvn "-Dmaven.repo.local=.m2/repository" test
```

前端构建：

```powershell
cd frontend
npm run build
```

后端健康检查：

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -Method Get
```

验证视频详情接口：

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/analysis/videos/BV1s3LA64ErQ/detail" -Method Get
```

验证 AI SSE 接口时，建议通过前端 AI 助手页面触发，因为浏览器端更容易观察流式输出效果。

## 常见问题

### 前端页面没有数据

先确认后端是否启动：

```text
http://localhost:8080/actuator/health
```

再确认 MySQL 中是否有 ADS 表数据：

```sql
select count(*) from ads_video_heat_rank;
select count(*) from ads_video_sentiment;
select count(*) from ads_danmaku_timeline;
```

### 任务中心刷新后没有任务

任务生成依赖 `ads_video_heat_rank`、`ads_video_sentiment`、`ads_danmaku_timeline`、`ads_up_performance` 等分析结果。若这些表为空或字段缺失，任务中心无法生成有效任务。

### 某些视频没有正向占比

通常是 `ads_video_sentiment` 中没有该 BVID 的情感聚合记录，或该视频缺少评论/文本分析样本。

### 视频详情没有关键词

优先检查聚合关键词表：

```sql
select *
from ads_keyword_top
where dimension_type = 'bvid'
  and dimension_value = '目标BVID';
```

如果聚合表为空，再检查文本分析明细：

```sql
select count(*)
from dws_text_analysis_detail
where bvid = '目标BVID'
  and keywords is not null
  and keywords <> '';
```

### AI 助手无响应或只返回配置提示

检查后端 Dify 配置：

```powershell
$env:DIFY_BASE_URL
$env:DIFY_API_KEY
```

配置后需要重启后端。

### 前端构建出现 Vite 写入 `.vite-temp` 权限错误

Windows 环境下如果出现 `EPERM: operation not permitted, open frontend\node_modules\.vite-temp\...`，通常是权限或文件占用问题。关闭占用进程后重试，或以具备写权限的终端运行构建。

### 图表布局不整齐

前端主要布局样式集中在 [frontend/src/styles/main.css](frontend/src/styles/main.css)。数据总览页由 `OverviewPage.vue` 组织，图表容器由 `ChartPanel.vue` 统一外观和高度。调整图表时应同时注意外层卡片高度、内部滚动区域和移动端断点。

## 已知约束

- 热度榜接口当前通常按 `limit=100` 加载样本，前端默认最多展示 100 条热度样本。
- 数据分区依赖源数据中的 `category` 字段。若源数据缺失，只能通过标题或 UP 主规则推断，准确性会下降。
- 当前趋势统计主要基于 `crawled_at` 等采集时间，未必等同于真实评论发布时间。
- 报表 Excel 文件由浏览器端即时生成，后端主要保存导出历史和元信息。
- 演示数据只用于功能验证，不代表真实业务分布。

## 安全说明

- 不要把真实 MySQL 密码、Dify API Key、SSH 密码或其他密钥提交到 Git。
- 生产环境建议通过环境变量、配置中心或密钥管理系统注入敏感配置。
- 若 `application.yml` 中存在本地调试默认值，部署前应替换为环境变量配置。
- `.env`、Hive 连接配置、数据导出配置中可能包含内网地址和账号信息，应避免公开传播。

## 后续可改进方向

- 完善分区字段链路：优先接入官方 `category_id` 或 `partition_name`，减少规则推断。
- 增加导入幂等和去重监控：按 BVID 建立唯一性约束，记录重复导入率和批次日志。
- 增强时间序列分析：补齐每日/每小时热度、评论、弹幕和情感趋势。
- 优化关键词质量：加入停用词过滤、实体词抽取和视频级长尾关键词。
- 强化异常检测：从固定阈值扩展到环比、同比、分位数和移动窗口异常。
- 完善权限体系：为任务中心、规则管理、报表导出等操作增加用户身份和权限控制。
- 增加可视化联动：图表点击后联动筛选视频、评论、弹幕和任务。
