# BiliLens Visualization Platform

BiliLens is a Bilibili content analytics dashboard. The backend exposes Spring Boot REST APIs over MySQL analysis tables, and the frontend renders a Vue 3 + Element Plus + ECharts operations dashboard.

## Project Structure

- `backend/` - Spring Boot analytics API
- `frontend/` - Vue dashboard
- `backend/sql/mysql/` - MySQL schema and placeholder data

## Backend

```powershell
cd backend
$env:MYSQL_URL="jdbc:mysql://localhost:3306/bilibili_analysis?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"
$env:MYSQL_USERNAME="root"
$env:MYSQL_PASSWORD="your_password"
mvn "-Dmaven.repo.local=.m2/repository" spring-boot:run
```

Run tests:

```powershell
cd backend
mvn "-Dmaven.repo.local=.m2/repository" test
```

## Frontend

Create a local env file from `frontend/.env.development.example` when the API is not served from the same origin.

```powershell
cd frontend
npm install
npm run dev
```

Build:

```powershell
cd frontend
npm run build
```

## Configuration

- `MYSQL_URL` - backend MySQL JDBC URL
- `MYSQL_USERNAME` - backend MySQL username, defaults to `root`
- `MYSQL_PASSWORD` - backend MySQL password, no committed default
- `VITE_API_BASE_URL` - optional frontend API base URL
- `VITE_API_TIMEOUT_MS` - frontend request timeout, defaults to `15000`
