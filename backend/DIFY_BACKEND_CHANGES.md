# 后端改动说明 (Dify AI助手集成)

## 改动概述

为集成 Dify AI 助手，在后端新增了一个视频推荐接口，供 Dify Chatflow 调用。所有改动均为**新增代码**，不修改任何现有接口和逻辑，**不影响原有展示端功能**。

---

## 改动文件清单

### 1. AnalysisRepository.java

**路径**: `backend/src/main/java/com/bililens/analytics/analysis/repository/AnalysisRepository.java`

**改动类型**: 新增方法

**新增方法**:
```java
public List<VideoHeatRankDto> findVideoHeatRankByCategory(String category, int limit)
```

**功能**: 按视频分区查询热度排行，支持不传分区返回全部。

**SQL 逻辑**:
- 传入 `category` 时: `WHERE category = :category ORDER BY heat_score DESC LIMIT :limit`
- 不传 `category` 时: `ORDER BY heat_score DESC LIMIT :limit` (返回全部)

**数据来源**: `ads_video_heat_rank` 表 (已有表，未修改表结构)

**改动量**: +31 行

---

### 2. AnalysisService.java

**路径**: `backend/src/main/java/com/bililens/analytics/analysis/service/AnalysisService.java`

**改动类型**: 新增方法

**新增方法**:
```java
@Cacheable(value = "videoHeatRankByCategory", key = "{#category, #limit}")
public List<VideoHeatRankDto> getVideosByCategory(String category, int limit)
```

**功能**: 调用 Repository 层查询，带缓存支持。

**缓存策略**:
- 缓存名称: `videoHeatRankByCategory`
- 缓存键: `{category, limit}` 组合
- 相同参数的请求会直接返回缓存，不查数据库

**改动量**: +4 行

---

### 3. AnalysisController.java

**路径**: `backend/src/main/java/com/bililens/analytics/analysis/controller/AnalysisController.java`

**改动类型**: 新增接口

**新增接口**:
```
GET /api/analysis/dify/recommend
```

**请求参数**:

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `category` | String | 否 | null | 视频分区，如"科技"、"游戏"、"音乐"等 |
| `limit` | int | 否 | 5 | 返回数量，范围 1-100 |

**请求示例**:
```bash
# 推荐科技区 Top 5 视频
GET /api/analysis/dify/recommend?category=科技&limit=5

# 返回全部热度 Top 5
GET /api/analysis/dify/recommend?limit=5
```

**响应格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "bvid": "BV1xx411c7mQ",
      "title": "视频标题",
      "upName": "UP主名称",
      "category": "科技",
      "viewCount": 100000,
      "likeCount": 5000,
      "coinCount": 3000,
      "favoriteCount": 4000,
      "replyCount": 2000,
      "danmakuCount": 1500,
      "heatScore": 95.5,
      "rankNo": 1
    }
  ]
}
```

**改动量**: +8 行

---

## 接口调用关系

```
Dify Chatflow (HTTP请求节点)
    │
    │ GET /api/analysis/dify/recommend?category=科技&limit=5
    ↓
AnalysisController.recommendVideos()
    │
    ↓
AnalysisService.getVideosByCategory()  ← 缓存检查
    │
    ↓ (缓存未命中)
AnalysisRepository.findVideoHeatRankByCategory()
    │
    ↓
MySQL: ads_video_heat_rank 表
```

---

## 对原有功能的影响

| 检查项 | 结果 | 说明 |
|--------|------|------|
| 现有接口 | ✅ 无影响 | 原有 7 个接口未修改 |
| 现有方法 | ✅ 无影响 | Service/Repository 原有方法未修改 |
| 数据库表 | ✅ 无影响 | 未修改任何表结构，只读查询 |
| 缓存 | ✅ 无影响 | 使用独立缓存名称 `videoHeatRankByCategory` |
| 前端展示 | ✅ 无影响 | 新增接口，原有前端调用不变 |

---

## Dify 端使用说明

### Chatflow HTTP 请求节点配置

| 配置项 | 值 |
|--------|-----|
| URL | `http://localhost:8080/api/analysis/dify/recommend` |
| 方法 | GET |
| 参数 | `category={{提取的分类}}`, `limit=5` |
| 超时 | 10 秒 |
| 重试 | 2 次 |

### 意图提取示例

用户输入: "推荐几个科技区的视频"
→ LLM 提取: `category = "科技"`
→ 调用: `GET /api/analysis/dify/recommend?category=科技&limit=5`

---

## 部署注意事项

### 本地部署
无需额外配置，后端启动后直接访问 `http://localhost:8080/api/analysis/dify/recommend`

### 分布式部署 (Dify 与后端在不同服务器)
需要在 `application.yml` 添加 CORS 配置:
```yaml
spring:
  web:
    cors:
      allowed-origins: "http://Dify服务器IP"
      allowed-methods: GET
      allowed-headers: "*"
      path-patterns: /api/analysis/dify/**
```

---

## 后续扩展

如需增加更多推荐维度，可在此接口基础上扩展:
- 按情感倾向推荐: 添加 `sentiment=positive` 参数
- 按 UP 主推荐: 添加 `upName=xxx` 参数
- 按关键词推荐: 结合 `ads_keyword_top` 表做关联查询
