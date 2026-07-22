# 通用视频数据可视化平台

本项目面向多个视频平台提供统一的数据分析能力。内容、账号、指标快照、时序互动、评论、回复和剧评均使用同一套 v2 数据模型。历史哔哩哔哩数据可以迁移，但不再是默认运行模式。

## 项目组成

- `frontend`：Vue 3、Vite、Element Plus、ECharts 前端界面。
- `backend`：提供 `/api/v2` 的 Spring Boot 服务。
- `bilibili_data_pipeline`：v2 JSONL 规范化与 MySQL 导入工具。目录名称为兼容历史保留，导入器可处理任意平台映射。

## 快速启动

新部署请创建通用 v2 数据库，不要对新库执行旧版 `schema.sql`：

```powershell
mysql -u root -p -e "create database if not exists video_analytics default character set utf8mb4 collate utf8mb4_unicode_ci;"
mysql -u root -p video_analytics < backend\sql\mysql\schema_v2.sql
```

可选导入演示数据：

```powershell
cd bilibili_data_pipeline
$env:ANALYTICS_DB_NAME="video_analytics"
python src\bootstrap_v2_mysql.py
```

启动后端：

```powershell
cd backend
mvn spring-boot:run
```

现有本地环境默认连接 `bilibili_analysis` 以保持兼容。新库请在启动前设置：

```powershell
$env:ANALYTICS_DB_NAME="video_analytics"
```

启动前端：

```powershell
cd frontend
npm install
npm run dev
```

访问地址：`http://localhost:5173`。

## 导入协议

所有来源先规范化为四个 JSONL 文件：

```text
contents.jsonl
accounts.jsonl
metric_snapshots.jsonl
interactions.jsonl
```

导入命令：

```powershell
cd bilibili_data_pipeline
$env:ANALYTICS_DB_NAME="video_analytics"
python src\import_v2_to_mysql.py --input-dir path\to\v2-export
```

CSV 来源可使用 `normalize_to_v2.py`。脚本内置历史哔哩哔哩字段映射，也支持其他平台提供 JSON 映射文件：

```powershell
python src\normalize_to_v2.py --input-dir path\to\csv --output-dir output\douyin `
  --platform-code douyin --connector-name douyin-approved-export-v1 `
  --mapping-file mappings\douyin.json
```

映射文件把 `content_id`、`title`、`account_id`、`comment_id`、`video_time_seconds` 和 `metrics` 等统一字段映射到 CSV 列。未知数值指标会写入 `extra_metrics`，导入后可在“指标对比”页面使用。

## 历史数据迁移

已有哔哩哔哩 ADS/DWD/DWS 数据库可执行：

```powershell
cd bilibili_data_pipeline
python src\bootstrap_v2_mysql.py --include-legacy
```

旧版接口已经移除。历史数据迁移到 v2 表后即可在当前应用中使用。

## 创建新库并复制现有数据

如果要创建 `video_analytics` 并完整保留当前 `bilibili_analysis` 中的 v2 数据，请依次执行：

```powershell
mysql -u root -p -e "create database if not exists video_analytics default character set utf8mb4 collate utf8mb4_unicode_ci;"
mysql -u root -p video_analytics < backend\sql\mysql\schema_v2.sql
mysql -u root -p < backend\sql\mysql\clone_v2_from_bilibili_analysis.sql
```

复制脚本仅复制 v2 内容、指标、互动、情感和运营数据，不会把旧 ADS/DWD 表复制到新库。

## 验证

```powershell
cd backend
mvn clean test

cd ..\frontend
npm run build

cd ..\bilibili_data_pipeline
python -m pytest tests
```
