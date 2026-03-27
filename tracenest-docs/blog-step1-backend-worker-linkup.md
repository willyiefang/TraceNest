# TraceNest 开发记录（Step 1）

> 主题：从“各自能跑”到“前后端闭环”  
> 阶段：v0（最小可运行）  
> 目标读者：第一次做 Java API + Node Worker 分仓协作的开发者

---

## 1. 这一步在做什么

### 1.1 阶段目标

- [ ] 让后端和 Worker 不再“各跑各的”
- [ ] 让 Worker 能通过 HTTP 创建任务并回传结果
- [ ] 形成最小闭环：`PENDING -> SUCCEEDED`

### 1.2 非目标（本阶段不做）

- [ ] 不接数据库（先用内存仓库）
- [ ] 不接 MQ / 对象存储
- [ ] 不做证据包签名与验证
- [ ] 不做 Playwright 真抓取（先占位产物）

---

## 2. 从 GET 到 POST：我做了什么

### 2.1 起点（只有 GET 探活）

- 后端只提供：
  - `GET /api/v1/health`
- Worker 只做：
  - 本地生成占位文件
  - 请求 health 判断“后端是否在线”

> 结论：这时只是“连通性验证”，不是业务闭环。

### 2.2 过渡（新增任务接口）

- 后端新增：
  - `POST /api/v1/capture-tasks`（创建任务，返回 taskId）
  - `GET /api/v1/capture-tasks/{taskId}`（查询任务）
  - `POST /internal/v1/worker/capture-tasks/{taskId}/finish`（Worker 回调完成）
- Worker 调整：
  1. 先 `POST /api/v1/capture-tasks`
  2. 再执行抓取（当前占位）
  3. 最后 `POST /internal/.../finish`

### 2.3 到达点（最小闭环）

- Worker 日志出现：
  - `creating backend task ...`
  - `finished task ... status: SUCCEEDED`
- 后端返回：
  - `taskId + status: SUCCEEDED`

---

## 3. 前后端链路（数据从哪到哪）

### 3.1 请求流

1. Worker -> API：创建任务  
   `POST /api/v1/capture-tasks`

2. API -> Worker：返回 `taskId`

3. Worker 本地执行抓取（占位）
   - 产物：html / screenshotText

4. Worker -> API：回调完成  
   `POST /internal/v1/worker/capture-tasks/{taskId}/finish`

5. API 更新任务状态为 `SUCCEEDED`

### 3.2 任务状态（v0）

- `PENDING`
- `SUCCEEDED`

> 后续可扩展：`RUNNING / FAILED / RETRYABLE / EXPIRED`

---

## 4. 这一步踩到的坑（可复用排障经验）

### 4.1 坑一：`mvnw.cmd` 启动后只输出 Java 用法

- 现象：
  - 执行 `.\mvnw.cmd spring-boot:run` 后不是 Spring 日志，而是 Java help
- 根因：
  - wrapper 脚本参数传递有问题（项目路径与参数解析）
- 处理：
  - 修复 `mvnw.cmd`
  - 用 `cmd /c mvnw.cmd spring-boot:run` 验证

### 4.2 坑二：`POST /api/v1/capture-tasks` 返回 404

- 现象：
  - health 通，create task 404
- 根因：
  - 运行实例不是最新代码（或路由未被正确加载）
- 处理：
  - 重启后端并确认启动日志
  - 先用浏览器 Console 单独测 POST，再跑 Worker

### 4.3 坑三：`finish` 回调 404

- 现象：
  - create 成功，finish 404
- 根因：
  - Controller 前缀与 internal 路径拼接错误
- 处理：
  - 将 Worker 回调拆到独立 internal controller
  - 确保路径严格匹配 `/internal/v1/worker/.../finish`

### 4.4 坑四：Node 端偶发 `UV_HANDLE_CLOSING` 断言

- 现象：
  - 进程退出时出现 `Assertion failed: !(handle->flags & UV_HANDLE_CLOSING)`
- 说明：
  - 常见于 Windows + 新版 Node 的退出阶段日志，不一定影响业务链路
- 临时策略：
  - 先以“接口结果是否成功”为准
  - 后续单独排查 Node 版本与退出钩子

---

## 5. 本阶段产出清单（可写到博客）

### 5.1 结构层

- [ ] `tracenest-be`（Spring Boot）
- [ ] `tracenest-worker`（Node Worker）
- [ ] `tracenest-infra`（docker-compose）
- [ ] `tracenest-docs`（文档）

### 5.2 能力层

- [ ] 后端可启动（Maven Wrapper）
- [ ] health 探活可用
- [ ] create / get / finish 三接口打通
- [ ] Worker 能创建任务并回调完成

---

## 6. 开发阶段定位（我现在处于哪一步）

当前属于：

- [x] **Step 1：最小链路联通（Local Happy Path）**
- [ ] Step 2：状态机完善（RUNNING/FAILED）
- [ ] Step 3：持久化（MySQL）与对象存储（MinIO）
- [ ] Step 4：异步化（MQ）与重试机制
- [ ] Step 5：证据包（manifest/hash/signature/verify）

---

## 7. 下一步计划（博客结尾可用）

### 7.1 短期（下一迭代）

- [ ] 增加 `RUNNING`、`FAILED` 状态
- [ ] 增加失败回调接口
- [ ] Worker 增加异常路径与重试

### 7.2 中期

- [ ] 接 MySQL（任务持久化）
- [ ] 接 MinIO（artifact 外部存储）
- [ ] 接 MQ（异步调度）

### 7.3 长期

- [ ] 引入可验证证据包（manifest + hash + signature）
- [ ] 增加 verify API 与审计日志

---

## 8. 可粘贴的关键日志样例

### 8.1 Worker 成功日志

```text
[worker] creating backend task for url=https://example.com
[worker] start url=https://example.com taskId=...
[worker] api reachable { service: 'tracenest-be', status: 'ok', ... }
[worker] finished task { taskId: '...', status: 'SUCCEEDED', ... }
```

### 8.2 API 创建任务成功样例

```json
{"taskId":"...","status":"PENDING"}
```

---

## 9. 复盘（可写主观总结）

- [ ] 这一阶段最关键的决策是什么？
- [ ] 最耗时的坑是什么？为什么？
- [ ] 如果重来一次，会先做什么？
- [ ] 下一阶段的风险点是什么？

