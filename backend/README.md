# 通用视频数据分析 API

Spring Boot 后端服务，提供平台无关的视频内容、互动、情感、指标、账号、任务和采集能力。

## 启动

```powershell
cd backend
mvn spring-boot:run
```

默认端口为 `8080`。现有本地环境默认使用 `bilibili_analysis`；新建通用库请设置以下环境变量：

```powershell
$env:ANALYTICS_DB_NAME="video_analytics"
$env:MYSQL_USERNAME="root"
$env:MYSQL_PASSWORD="你的密码"
mvn spring-boot:run
```

也可用 `MYSQL_URL` 完整覆盖 JDBC 地址。

## 主要接口

- `GET /api/v2/platforms`
- `GET /api/v2/contents`
- `GET /api/v2/contents/page`
- `GET /api/v2/contents/{contentId}`
- `GET /api/v2/contents/{contentId}/interactions`
- `GET /api/v2/contents/{contentId}/sentiment`
- `GET /api/v2/contents/{contentId}/timeline`
- `GET /api/v2/analytics/metric-definitions`
- `GET /api/v2/analytics/metric-comparison`
- `GET /api/v2/analytics/comments/summary`
- `GET /api/v2/accounts/performance`
- `GET /api/tasks`
- `GET /api/platform/data-sources/status`
- `GET /actuator/health`

## 数据库

新部署使用 `sql/mysql/schema_v2.sql` 创建 v2 通用模型。旧版 B 站 ADS/DWD 表仅支持通过数据管道迁移到 v2，不再由 API 提供查询服务。

## 测试

```powershell
mvn clean test
```

测试使用 H2 内存数据库；生产环境使用 MySQL。
