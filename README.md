# BiLens — 多平台视频内容分析平台

**BiLens** 是一套面向多视频平台（B 站、抖音、爱奇艺、优酷等）的内容分析可视化系统。以统一数据模型为核心，不依赖单一平台的专有概念，实现跨平台内容可比。

## 系统架构

```
                    ┌───────────────────┐
                    │   MySQL 统一数仓    │
                    │  video_analytics   │
                    └────────┬──────────┘
                             │
             ┌───────────────┼───────────────┐
             │               │               │
             ▼               ▼               ▼
  ┌───────────────────┐ ┌──────────────────┐
  │  Spring Boot      │ │  Dify AI 助手    │
  │  REST API         │ │  LLM 问答分析    │
  │  内容·指标·互动·情感 │ │                  │
  └─────────┬─────────┘ └──────────────────┘
            │
            ▼
  ┌────────────────────────────────────┐
  │  Vue 3 可视化仪表盘 (Element Plus)  │
  │  总览 · 内容 · 指标 · 评论 · 创作者   │
  │  采集 · 报表 · AI 助手             │
  └────────────────────────────────────┘
```

## 模块说明

| 模块 | 目录 | 技术栈 | 职责 |
|------|------|--------|------|
| **后端 API** | `backend/` | Spring Boot 4, Java 17, MySQL 8 | 提供 RESTful 查询接口：内容、指标快照、互动、情感分析、创作者画像、采集任务代理 |
| **前端仪表盘** | `frontend/` | Vue 3, Element Plus, ECharts, Vite | 多页面可视化：总览面板、内容列表与详情、指标对比、评论分析、创作者画像、数据采集、报表导出、AI 助手 |
| **AI 能力** | `hrbust-dify-re/` | Dify (LLM 应用平台) | Fork 自 Dify，为 AI 助手提供 LLM 对话、知识库、工作流编排能力 |
| **文档/协议** | `docs/` | — | 统一数据协议 v2 |

## 核心能力

- **多平台统一数据模型** — 平台特有字段保留在原始属性或扩展指标，核心维度（内容、账号、指标、互动）跨平台兼容
- **内容全维度分析** — 指标快照对比、互动情感分析、弹幕/评论时间文本降级展示
- **统一指标字典** — 所有指标带定义和跨平台可比性标记，不支持字段保持 NULL 而非填零
- **平台热度归一化** — 支持平台内排行和跨平台百分位比较
- **AI 辅助分析** — 集成 LLM 能力，支持自然语言问答式数据分析
- **Excel 导出** — 当前页面数据一键导出

## 快速开始

### 前置条件

- JDK 17
- Node.js 18+
- MySQL 8.0

### 1. 创建数据库

```powershell
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS video_analytics DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p video_analytics < backend\sql\mysql\schema_v2.sql
```

### 2. 启动后端

```powershell
cd backend
$env:ANALYTICS_DB_NAME = "video_analytics"
$env:MYSQL_USERNAME = "root"
$env:MYSQL_PASSWORD = "your_password"
mvn clean spring-boot:run
```

后端地址：`http://localhost:8080`
健康检查：`http://localhost:8080/actuator/health`

### 3. 启动前端

```powershell
cd frontend
npm install
npm run dev
```

访问 `http://localhost:5173`。

## 数据库模型

采用星型模式设计，五张核心表：

| 表 | 类型 | 说明 |
|----|------|------|
| `dim_platform` | 维度 | 平台定义与能力声明 |
| `dim_account` | 维度 | 创作者/账号信息 |
| `dim_content` | 维度 | 视频、剧集等内容元数据 |
| `fact_content_metric_snapshot` | 事实 | 指标快照（播放、点赞、评论、分享等） |
| `fact_interaction` | 事实 | 互动数据（评论、弹幕、剧评、回复） |
| `fact_text_analysis` | 事实 | 文本分析结果（情感、关键词） |

完整表结构见 [`backend/sql/mysql/schema_v2.sql`](backend/sql/mysql/schema_v2.sql)。

## 数据流

```text
原始数据 ──▶ 统一数仓 ──▶ REST API ──▶ 前端仪表盘
                             │
                          Dify AI 助手
```

数据合同与质量要求见 [`docs/unified_data_contract_v2.md`](docs/unified_data_contract_v2.md)。

## 前端页面

| 路由 | 页面 | 说明 |
|------|------|------|
| `/overview` | 总览面板 | 核心指标看板、趋势图 |
| `/contents` | 内容列表 | 全平台内容检索与筛选 |
| `/contents/:id` | 内容详情 | 指标、互动、情感、时间文本分析 |
| `/contents/compare` | 内容对比 | 跨内容指标对比 |
| `/metrics` | 指标探索 | 指标字典与趋势分析 |
| `/comment` | 评论分析 | 评论情感与关键词洞察 |
| `/creator` | 创作者画像 | 创作者维度分析 |
| `/collector` | 数据采集 | 爬虫任务管理与配置 |
| `/reports` | 报表中心 | 报表生成与导出 |
| `/ai-assistant` | AI 助手 | 自然语言数据分析 |

## 验证

```powershell
# 后端测试
cd backend
mvn clean test

# 前端构建验证
cd frontend
npm run build
```

## 参与开发

各模块的详细说明和开发指南见各自目录下的 README：

- [后端 API](backend/README.md) — Spring Boot 接口文档
- [AI 平台](hrbust-dify-re/README.md) — Dify 集成说明（英文）
