# BiliLens Analytics API

Spring Boot RESTful API for returning analyzed Bilibili dashboard data.

## Run

```bash
mvn "-Dmaven.repo.local=.m2/repository" spring-boot:run
```

The API starts on `http://localhost:8080`.

Production runtime uses MySQL. Configure the connection with environment variables:

```bash
MYSQL_URL="jdbc:mysql://localhost:3306/bilibili_analysis?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"
MYSQL_USERNAME="root"
MYSQL_PASSWORD="your_password"
```

Windows PowerShell example:

```powershell
$env:MYSQL_URL="jdbc:mysql://localhost:3306/bilibili_analysis?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"
$env:MYSQL_USERNAME="root"
$env:MYSQL_PASSWORD="your_password"
mvn "-Dmaven.repo.local=.m2/repository" spring-boot:run
```

## Test

```bash
mvn "-Dmaven.repo.local=.m2/repository" test
```

## Endpoints

- `GET /api/analysis/videos/heat-rank?limit=10`
- `GET /api/analysis/videos/sentiment`
- `GET /api/analysis/sentiment/trend?startDate=2026-05-01&endDate=2026-06-01`
- `GET /api/analysis/danmaku/timeline?bvid=BV001`
- `GET /api/analysis/keywords?dimensionType=global&dimensionValue=all&limit=30`
- `GET /api/analysis/ups/performance?limit=10`
- `GET /api/analysis/comments/negative?bvid=BV001&limit=20`
- `GET /actuator/health`

## MySQL Schema

Use these scripts for your real MySQL database:

- `sql/mysql/schema.sql`
- `sql/mysql/placeholder-data.sql`

`placeholder-data.sql` contains placeholder rows only. Replace them with your real CSV imported data or edit the values manually.

The automated tests still use H2 under the `test` profile only:

- `src/test/resources/application-test.yml`
- `src/test/resources/schema.sql`
- `src/test/resources/data.sql`
