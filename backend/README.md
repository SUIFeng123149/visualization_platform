# Multi-platform Video Analytics API

Spring Boot RESTful API for platform-neutral video analytics. Legacy Bilibili endpoints are disabled by default.

## Run

```bash
cd backend
mvn spring-boot:run
```

The API starts on `http://localhost:8080`.

Production runtime uses MySQL. Configure the connection with environment variables:

```bash
MYSQL_URL="jdbc:mysql://localhost:3306/bilibili_analysis?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"
MYSQL_USERNAME="root"
MYSQL_PASSWORD="your_password"
```

`MYSQL_USERNAME` defaults to `root`; the development password remains `123456` for compatibility and should be overridden outside source control.

Windows PowerShell example:

```powershell
$env:MYSQL_URL="jdbc:mysql://localhost:3306/bilibili_analysis?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"
$env:MYSQL_USERNAME="root"
$env:MYSQL_PASSWORD="your_password"
mvn spring-boot:run
```

The project-level `.mvn/settings.xml` stores dependencies in `backend/.m2/repository`, avoiding an unwritable global Maven repository. Use `mvn -o spring-boot:run` when dependencies are already cached and the network is unavailable.

## Test

```bash
mvn -o test
```

## Endpoints

- `GET /api/v2/platforms`
- `GET /api/v2/contents?platform=bilibili&contentType=video`
- `GET /api/v2/contents/{contentId}`
- `GET /api/v2/contents/{contentId}/children`
- `GET /api/v2/contents/{contentId}/interactions?type=comment`
- `GET /api/v2/contents/{contentId}/sentiment`
- `GET /api/v2/contents/{contentId}/timeline?type=danmaku`
- `GET /api/v2/analytics/trends?platform=douyin`
- `GET /api/v2/analytics/keywords?platform=iqiyi`
- `GET /api/v2/analytics/comments/summary?platform=douyin&type=comment`
- `GET /api/v2/analytics/comments/types?platform=douyin&startDate=2026-05-01&endDate=2026-05-31`
- `GET /api/v2/analytics/comments/trends?platform=iqiyi&type=review&startDate=2026-05-01&endDate=2026-05-31`
- `GET /api/v2/analytics/comments/negative?platform=youku&page=1&pageSize=10`
- `GET /api/v2/accounts/performance?platform=bilibili`

- `GET /actuator/health`

## MySQL Schema

Use these scripts for your real MySQL database:

- `sql/mysql/schema.sql`
- `sql/mysql/placeholder-data.sql`
- `sql/mysql/migration_v2_unified.sql`

`placeholder-data.sql` contains placeholder rows only. Replace them with your real CSV imported data or edit the values manually.

Run `migration_v2_unified.sql` after `schema.sql` for every local database. The default UI uses `/api/v2` and requires the unified tables.

`/api/analysis/**`, `ads_*`, and `dwd_*` are deprecated compatibility surfaces. New features must use `/api/v2`, `dim_content`, `fact_content_metric_snapshot`, and `fact_interaction` with `content_id` as the business key. Set `ANALYTICS_LEGACY_ENABLED=true` only when operating an existing Bilibili ADS deployment during migration.

The automated tests still use H2 under the `test` profile only:

- `src/test/resources/application-test.yml`

The H2 fixtures model Bilibili comments/danmaku, a Douyin short video, and an iQIYI series/episode. Run `mvn -o test` to validate the full v2 logic before real tables are available.
- `src/test/resources/schema.sql`
- `src/test/resources/data.sql`
