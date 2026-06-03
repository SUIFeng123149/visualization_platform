# BiliLens 可视化分析平台

BiliLens 是一个面向 B 站内容运营场景的数据可视化平台。项目通过后端 API 聚合视频热度、评论情感、弹幕时间轴、关键词和 UP 主表现等分析结果，并在前端以数据看板的形式提供浏览、筛选、复盘和报表导出能力。

## 项目定位

本项目适合作为内容数据分析、舆情洞察、运营复盘和可视化课程设计的基础平台。当前版本聚焦“分析结果展示”和“运营决策辅助”，默认使用 MySQL 存放分析宽表和明细表，后端提供 REST API，前端负责交互式可视化呈现。

## 技术栈

- 前端：Vue 3、Vite、Element Plus、ECharts
- 后端：Java 21、Spring Boot 4、Spring JDBC、Spring Validation、Actuator
- 数据库：MySQL，测试环境使用 H2
- 构建工具：npm、Maven

## 目录结构

```text
visualization_platform/
├── backend/                 # Spring Boot 后端服务
│   ├── src/main/java/        # API、Service、Repository、DTO
│   ├── src/main/resources/   # 后端配置
│   ├── src/test/             # 后端集成测试和 H2 测试数据
│   └── sql/mysql/            # MySQL 建表脚本和占位数据
├── frontend/                # Vue 前端看板
│   ├── src/api/              # API 请求封装
│   ├── src/components/       # 布局、指标、图表、表格组件
│   ├── src/composables/      # ECharts 组合式逻辑
│   ├── src/data/             # 导航和运营建议静态配置
│   ├── src/styles/           # 全局样式
│   └── src/utils/            # 图表工具、Excel 导出工具
└── README.md                # 项目总说明
```

## 核心功能

- 数据总览：展示播放量、互动量、平均情感、最高热度等关键指标。
- 视频分析：按热度、互动率、正向占比查看视频表现。
- 弹幕分析：用散点图定位弹幕高峰片段，辅助剪辑和复盘。
- 评论洞察：展示评论情感占比、情感趋势和负面评论样本。
- UP 主画像：用雷达图和排行表对比创作者表现。
- 任务中心：沉淀基于数据洞察的运营动作建议。
- 报表导出：按模块导出 Excel 格式分析报表。

## 环境要求

- Node.js 18 或更高版本
- npm 9 或更高版本
- JDK 21
- Maven 3.9 或更高版本
- MySQL 8 或兼容版本

## 数据库初始化

先创建并初始化 MySQL 数据库：

```powershell
cd backend
mysql -u root -p < sql/mysql/schema.sql
mysql -u root -p bilibili_analysis < sql/mysql/placeholder-data.sql
```

`placeholder-data.sql` 只包含占位样例数据。接入真实数据时，应替换为爬虫、清洗、情感分析和聚合计算后的结果表数据。

## 后端启动

后端默认端口为 `8080`。生产或本地连接 MySQL 时，通过环境变量配置数据库连接：

```powershell
cd backend
$env:MYSQL_URL="jdbc:mysql://localhost:3306/bilibili_analysis?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"
$env:MYSQL_USERNAME="root"
$env:MYSQL_PASSWORD="your_password"
mvn "-Dmaven.repo.local=.m2/repository" spring-boot:run
```

健康检查：

```text
GET http://localhost:8080/actuator/health
```

## 前端启动

前端默认使用 Vite 开发服务器，端口通常为 `5173`。开发环境下已配置 `/api` 代理到 `http://localhost:8080`。

```powershell
cd frontend
npm install
npm run dev
```

访问：

```text
http://localhost:5173
```

如果前后端不是同源部署，可以基于示例文件创建本地环境配置：

```powershell
cd frontend
copy .env.development.example .env.development
```

常用前端环境变量：

```env
VITE_API_BASE_URL=
VITE_API_TIMEOUT_MS=15000
```

## 构建与测试

后端测试：

```powershell
cd backend
mvn "-Dmaven.repo.local=.m2/repository" test
```

前端生产构建：

```powershell
cd frontend
npm run build
```

前端本地预览：

```powershell
cd frontend
npm run preview
```

## API 概览

后端主要接口统一以 `/api/analysis` 开头：

- `GET /api/analysis/videos/heat-rank?limit=10`
- `GET /api/analysis/videos/sentiment`
- `GET /api/analysis/sentiment/trend?startDate=2026-05-01&endDate=2026-06-01`
- `GET /api/analysis/danmaku/timeline?bvid=BV001`
- `GET /api/analysis/keywords?dimensionType=global&dimensionValue=all&limit=30`
- `GET /api/analysis/ups/performance?limit=10`
- `GET /api/analysis/comments/negative?bvid=BV001&limit=20`

接口响应统一包装为：

```json
{
  "code": 200,
  "message": "success",
  "data": []
}
```

## 配置说明

后端环境变量：

- `MYSQL_URL`：MySQL JDBC 连接地址。
- `MYSQL_USERNAME`：MySQL 用户名，默认 `root`。
- `MYSQL_PASSWORD`：MySQL 密码，源码中不提供默认密码。

前端环境变量：

- `VITE_API_BASE_URL`：接口基础地址。为空时默认请求当前站点同源接口。
- `VITE_API_TIMEOUT_MS`：前端请求超时时间，默认 `15000` 毫秒。

## 当前工程特性

- 前端请求封装包含超时、网络错误处理和统一业务响应解析。
- 页面提供加载失败提示和重试入口。
- 图表和表格提供空数据状态，避免接口异常时出现空白区域。
- ECharts、Element Plus 组件、图标和样式已按需引入。
- Vite 构建已拆分 ECharts、Element Plus 和通用 vendor chunk。
- 后端对请求参数做基础校验，并对日期范围错误返回 400。
- 后端集成测试使用 H2 独立数据，不依赖本地 MySQL。

## 后续可完善方向

- 增加登录鉴权和角色权限。
- 增加接口文档，例如 OpenAPI/Swagger。
- 增加数据导入流程，将 CSV、爬虫数据或离线分析结果接入 MySQL。
- 增加更多筛选维度，例如分区、UP 主、视频、日期范围和关键词。
- 增加详情抽屉，支持从视频、弹幕高峰、负面评论下钻到明细。
- 增加 CI 流程，自动运行后端测试和前端构建。
- 增加 Docker Compose，一键启动 MySQL、后端和前端。

## 注意事项

- 不要将真实数据库密码写入源码或提交到 Git。
- `backend/sql/mysql/placeholder-data.sql` 仅用于演示，不代表真实业务数据。
- 根目录如果出现无对应 `package.json` 的 `package-lock.json`，通常是误在根目录执行 npm 命令生成的文件，需要按团队约定确认是否保留。
