# 通用视频网站数据可视化平台

面向多视频平台的内容、互动、情感和账号数据分析系统。平台以统一数据模型为核心，不依赖 BVID、UP 主、投币等单一平台概念；平台特有字段保留在原始属性或扩展指标中。

## 系统边界

| 组件 | 职责 |
| --- | --- |
| 爬虫平台 | 按 URL 创建任务，管理认证档案，提供视频指标、评论与带时间文本数据。 |
| 数据清洗与分析 | 保存原始响应，转换 v2 JSONL，计算情感、关键词和归一化热度，导入 MySQL。 |
| 本项目后端 | 查询标准数据、提供分析 API、平台代理爬虫任务和配置管理。 |
| 本项目前端 | 展示内容分析、指标对比、互动洞察、账号画像和报表导出。 |

完整字段、入库顺序和验收标准见 [数据清洗与分析对接指南](docs/数据清洗与分析对接指南.md)。

## 主要能力

- 多平台内容、账号、指标快照和互动数据查询。
- 内容复盘：指标、互动情感、弹幕/字幕等时间文本按平台能力降级展示。
- 统一指标字典与跨平台可比性标记。
- 评论、回复、剧评和弹幕的情感、关键词分析。
- 平台、指标、预警规则和报表历史管理。
- Excel 导出当前页面数据；爬虫任务因上游未提供任务列表接口，不支持历史导出。
- 爬虫平台代理：认证档案创建/校验/启停，任务创建/查询/取消/恢复。

## 目录

```text
frontend/                    Vue 3 可视化界面
backend/                     Spring Boot API 与 MySQL 查询层
backend/sql/mysql/schema_v2.sql  新库完整结构和基础字典
docs/                        数据协议与对接文档
openapi(2).json              爬虫平台 OpenAPI 定义
```

## 新环境启动

### 1. 创建数据库

```powershell
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS video_analytics DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p video_analytics < backend\sql\mysql\schema_v2.sql
```

For an existing v2 database, run the performance migration once:

```powershell
mysql -u root -p video_analytics < backend\sql\mysql\20260723_add_metric_content_latest_index.sql
```

### 2. 配置并启动后端

```powershell
cd backend
$env:ANALYTICS_DB_NAME = "video_analytics"
$env:MYSQL_USERNAME = "root"
$env:MYSQL_PASSWORD = "你的数据库密码"

# 爬虫服务可用时再设置；未设置时采集页会显示“暂未接入”。
$env:CRAWLER_BASE_URL = "http://crawler-host:port"
$env:CRAWLER_API_KEY = "可选的 API Key"

mvn clean spring-boot:run
```

后端地址：`http://localhost:8080`，健康检查：`http://localhost:8080/actuator/health`。

> IDE 必须使用 JDK 17。若出现 `class file version 65.0`，停止 IDE 的 Java 21 自动编译，执行 `mvn clean` 后重新启动。

### 3. 启动前端

```powershell
cd frontend
npm install
npm run dev
```

访问 `http://localhost:5173`。

## 数据交接

清洗分析团队每个批次交付以下四个 UTF-8 JSONL 文件：

```text
accounts.jsonl
contents.jsonl
metric_snapshots.jsonl
interactions.jsonl
text_analyses.jsonl  # 有可分析互动文本时提供
```

数据合同、质量要求和交付清单见 [数据清洗与分析对接指南](docs/数据清洗与分析对接指南.md)。本项目不负责清洗分析实现；平台侧按交付合同安排入库。不要向新库写入旧版 ADS/DWD/DWS 表，也不要把不支持的指标填为 `0`。

## 验证

```powershell
cd backend
mvn clean test

cd ..\frontend
npm run build
```

## 爬虫接口约束

本项目按根目录 `openapi(2).json` 对接爬虫平台。上游当前不提供关键词搜索、账号搜索、热门内容、任务列表和直接导入接口，因此界面不会展示这些功能。采集结果须由数据清洗与分析链路完成规范化和入库后，才会出现在可视化页面。
