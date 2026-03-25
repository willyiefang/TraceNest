# 第一步开发思路（v0 → v1 可跑闭环）

本文档描述 TraceNest 当前阶段“第一步应该怎么做、为什么这么做、做到什么程度算完成”。

## 目标

第一步只追求一件事：**把工程跑起来 + 跑通最小闭环**。

最小闭环定义为：

- **后端（Java）**能启动并提供一个稳定的探活接口（用于联调与排障）。
- **Worker（Node）**能执行一次“抓取任务”（当前可先用占位产物），并能确认后端可达。

> 这个阶段刻意不引入：数据库表、MQ、对象存储、真实浏览器抓取、证据包签名与校验。

## 为什么要这样拆

- **先跑起来**：工程结构/依赖/运行方式不稳定时，越早暴露问题越省时间。
- **先保证联调通路**：Worker ↔ API 的链路一旦通了，后续替换“占位抓取”为 Playwright、替换“探活”为真实回调都很顺滑。
- **先把目录分层放好**：后续加功能不会乱长成一个大文件堆。

## 当前仓库结构（已落地）

顶层目录：

- `tracenest-be/`：后端主服务（Spring Boot）
- `tracenest-worker/`：抓取 Worker（Node）
- `tracenest-infra/`：本地基础设施（docker-compose，占位备用）
- `tracenest-docs/`：文档与协议说明

后端 `tracenest-be` 当前具备：

- `pom.xml`：Spring Boot Web 最小依赖
- `mvnw.cmd / mvnw`：**Maven Wrapper**（跨机器可重复构建，无需全局 Maven）
- `GET /api/v1/health`：探活接口
- 模块目录（当前多为空目录，用来承接后续代码）：
  - `auth/`、`casefile/`、`capture/`、`evidence/`、`storage/`、`audit/`、`ops/`

Worker `tracenest-worker` 当前具备：

- `node src/app.js` 可运行入口
- `captureService.js`：生成占位产物（html + screenshot.txt）
- `uploadClient.js`：检查后端 `GET /api/v1/health` 是否可达
- 分层目录（当前为空目录，用来承接后续实现）：
  - `adapters/`、`pipeline/`、`uploader/`、`reporter/`、`retry/`

## 第一步要做的“最小实现”清单

### 后端（必须）

- **能启动**：通过 Maven Wrapper 启动 Spring Boot
- **能探活**：`GET /api/v1/health` 返回 JSON（包含 status/timestamp 等）

完成标准：

- 在本机访问 `http://localhost:8080/api/v1/health` 能得到 `status=ok`

### Worker（必须）

- **能运行一次任务**：给定 URL 与 taskId，能产生本地产物文件
- **能确认后端在线**：能请求 health 接口并打印结果

完成标准：

- 运行后在 `tracenest-worker/output/<taskId>/` 看到 `page.html` 与 `screenshot.txt`
- 控制台输出包含 “api reachable …”

## 运行方式（第一步验收）

后端（Windows）：

- 进入 `tracenest-be` 目录
- 执行：`.\mvnw.cmd spring-boot:run`

Worker：

- 进入 `tracenest-worker` 目录
- 设置环境变量（PowerShell 示例）：
  - `$env:API_BASE_URL="http://localhost:8080"`
  - `$env:TARGET_URL="https://example.com"`
  - `$env:TASK_ID="local-dev-task-1"`
- 执行：`node src/app.js`

## 第一阶段结束后，第二步自然升级点（不在本阶段做）

当第一步验收通过后，第二步建议升级为真正的“任务闭环”：

- 后端加 `POST /tasks`（创建任务）与 `POST /internal/tasks/{id}/finish`（Worker 回调）
- Worker 从后端拿任务、回传任务结果（替代当前仅探活）
- 再往后再引入：MinIO 存储、MySQL 元数据、证据包 manifest/hash/signature

