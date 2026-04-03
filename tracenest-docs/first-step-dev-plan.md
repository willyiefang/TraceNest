# Step1：数据传输流与伪码（Worker JS + 后端 BE）

本文档只讲一件事：`**url`、`taskId`、请求体从哪来、传到哪、后端改什么状态。**  
读的时候按序号走，不要来回跳文件。

---

## 一、变量与数据来源（先全部 declared）

### 1.1 Worker 侧（运行 `node src/app.js` 时）


| 变量名                         | 类型            | 来源                                                                                                    |
| --------------------------- | ------------- | ----------------------------------------------------------------------------------------------------- |
| `API_BASE_URL`              | string        | 环境变量 `process.env.API_BASE_URL`；未设时默认 `"http://localhost:8080"`（见 `uploadClient.js` 的 `apiBaseUrl()`） |
| `TARGET_URL`                | string        | 环境变量 `process.env.TARGET_URL`；未设时默认 `"https://example.com"`（见 `app.js`）                               |
| `TASK_ID` / `backendTaskId` | string | null | 环境变量 `process.env.TASK_ID`；**不设则为 null**，后面由 create 接口返回值赋给 `backendTaskId`                           |
| `workerVersion`             | string        | 代码里常量，当前为 `"worker-v0"`（`app.js`）                                                                     |
| `created.taskId`            | string        | **仅当**调用了 create 后，来自后端 JSON：`{ "taskId": "...", "status": "PENDING" }`                               |


抓取产物（当前占位实现）：


| 字段               | 来源                                               |
| ---------------- | ------------------------------------------------ |
| `capturedAt`     | `captureService.js` 里 `new Date().toISOString()` |
| `html`           | 占位 HTML 字符串（`captureService.js` 生成）              |
| `screenshotText` | 占位截图文本（`captureService.js` 生成）                   |


### 1.2 后端侧（BE）


| 字段       | 含义                                                |
| -------- | ------------------------------------------------- |
| `taskId` | 后端 `UUID.randomUUID()` 生成，create 时入库/入内存          |
| `url`    | create 请求体里的 `url`，与 Worker 的 `TARGET_URL` 一致     |
| `status` | 枚举：`PENDING` → `RUNNING` → `SUCCEEDED` 或 `FAILED` |


---

## 二、HTTP 路径一览（无循环，顺序执行）

```
POST /api/v1/capture-tasks
POST /internal/v1/worker/capture-tasks/{taskId}/start
POST /internal/v1/worker/capture-tasks/{taskId}/finish
POST /internal/v1/worker/capture-tasks/{taskId}/fail   （仅异常路径）
GET  /api/v1/health                                   （探活，不改任务状态）
```

`taskId` 出现在 URL 路径里，**必须与** create 返回的 `taskId` 相同（Worker 里通常叫 `backendTaskId`）。

---

## 三、单次成功运行：数据传输时间线（从哪传到哪）

### 步骤 0：Worker 读取环境

```
API_BASE_URL = env.API_BASE_URL 或默认 "http://localhost:8080"
TARGET_URL   = env.TARGET_URL   或默认 "https://example.com"
backendTaskId = env.TASK_ID 或 null
```

对应文件：`tracenest-worker/src/app.js` 开头。

---

### 步骤 1：create（`url` 第一次进后端）

**Worker 发起**

- 函数：`createCaptureTask(url)`，文件：`tracenest-worker/src/uploadClient.js`
- 若 `app.js` 里 `backendTaskId` 为 null，则先调用 create。
- 请求：
  - `POST {API_BASE_URL}/api/v1/capture-tasks`
  - Header：`Content-Type: application/json`
  - **Body（JSON）**：`{ "url": TARGET_URL }`  
  即 `JSON.stringify({ url })`，其中 `url` 实参 = `app.js` 里的 `targetUrl`。

**后端接收**

- 类：`CaptureTaskController`，文件：`tracenest-be/.../CaptureTaskController.java`
- 方法：`@PostMapping("/capture-tasks")`（类上有 `@RequestMapping("/api/v1")`，故完整路径为 `/api/v1/capture-tasks`）
- 请求体映射到：`CreateCaptureTaskRequest`，字段 `url`。

**后端响应 → Worker**

```json
{ "taskId": "<uuid>", "status": "PENDING" }
```

**Worker 赋值**

```
backendTaskId = response.taskId
```

---

### 步骤 2：start（`taskId` 进 URL，时间进 Body）

**Worker 发起**

- 函数：`startCaptureTask(backendTaskId, startPayload)`，`uploadClient.js`
- 请求：
  - `POST {API_BASE_URL}/internal/v1/worker/capture-tasks/{backendTaskId}/start`
  - **Body**：
    ```json
    {
      "startedAt": "<ISO8601>",
      "workerVersion": "worker-v0"
    }
    ```

**后端接收**

- 类：`WorkerCaptureCallbackController`，路径前缀 `@RequestMapping("/internal/v1/worker/capture-tasks")`
- 方法：`@PostMapping("/{taskId}/start")`
- `taskId`：来自 URL 路径 `@PathVariable`
- Body：`StartCaptureTaskRequest`（`startedAt`, `workerVersion`）

**后端状态变更**

- 要求当前 `status == PENDING`
- 置 `status = RUNNING`，写入 `startedAt`，`repository.save(task)`

---

### 步骤 3：capture（不经过 HTTP，仅在 Worker 本机）

- 函数：`runCapture(backendTaskId, TARGET_URL)`，文件：`tracenest-worker/src/captureService.js`
- 产出：目录 `output/{backendTaskId}/` 下写文件；返回值里含 `capturedAt`, `html`, `screenshotText` 等。

---

### 步骤 4：finish（抓取结果进 Body，`taskId` 仍在 URL）

**Worker 发起**

- 函数：`finishCaptureTask(backendTaskId, finishPayload)`，`uploadClient.js`
- 请求：
  - `POST {API_BASE_URL}/internal/v1/worker/capture-tasks/{backendTaskId}/finish`
  - **Body**：
    ```json
    {
      "capturedAt": "<result.capturedAt>",
      "html": "<result.html>",
      "screenshotText": "<result.screenshotText>",
      "workerVersion": "worker-v0"
    }
    ```

**后端接收**

- `WorkerCaptureCallbackController`：`@PostMapping("/{taskId}/finish")`
- Body：`FinishCaptureTaskRequest`（`capturedAt`, `html`, `screenshotText`, `workerVersion`）

**后端状态变更**

- 允许 `PENDING` 或 `RUNNING`
- 置 `status = SUCCEEDED`，写入 `capturedAt/html/screenshotText`，`save`

---

### 步骤 5：fail（仅 `runCapture` 抛错时）

**Worker 发起**

- `failCaptureTask(backendTaskId, failPayload)`，`uploadClient.js`
- `POST .../{backendTaskId}/fail`
- **Body**：
  ```json
  {
    "failedAt": "<ISO8601>",
    "errorType": "CAPTURE_ERROR",
    "message": "<错误信息>",
    "workerVersion": "worker-v0"
  }
  ```

**后端**：`failTask`，置 `FAILED`，写 `failedAt`、`failureType`、`failureMessage`。

---

## 四、命名对照（避免和代码对不上）


| 口语          | Worker 里常见变量                     | 后端路径/字段                                   |
| ----------- | -------------------------------- | ----------------------------------------- |
| 要抓的网址       | `targetUrl`，来自 `TARGET_URL`      | create body 的 `url`                       |
| 任务编号        | `backendTaskId`（或环境变量 `TASK_ID`） | URL 里的 `{taskId}`                         |
| 完成时带的页面内容   | `result.html`                    | `FinishCaptureTaskRequest.html`           |
| 完成时带的截图占位文本 | `result.screenshotText`          | `FinishCaptureTaskRequest.screenshotText` |


---

## 五、Worker 主流程伪码（与 `app.js` 一致）

```
API_BASE_URL = env 或默认
TARGET_URL = env 或默认
backendTaskId = env.TASK_ID 或 null

if backendTaskId == null:
    createResp = HTTP POST /api/v1/capture-tasks, body = { url: TARGET_URL }
    backendTaskId = createResp.taskId

HTTP POST /internal/.../capture-tasks/{backendTaskId}/start,
    body = { startedAt: nowISO(), workerVersion }

try:
    result = runCapture(backendTaskId, TARGET_URL)
    HTTP POST /internal/.../capture-tasks/{backendTaskId}/finish,
        body = { capturedAt, html, screenshotText, workerVersion }
catch:
    HTTP POST /internal/.../capture-tasks/{backendTaskId}/fail,
        body = { failedAt, errorType, message, workerVersion }
```

---

## 六、后端状态机伪码（与 `WorkerCaptureCallbackController` + `CaptureTaskController` 一致）

```
create(url):
  taskId = newUUID()
  task = { taskId, url, status: PENDING }
  save(task)
  return { taskId, status: PENDING }

start(taskId, { startedAt, workerVersion }):
  task = load(taskId)
  require task.status == PENDING
  task.status = RUNNING
  task.startedAt = startedAt
  save(task)

finish(taskId, { capturedAt, html, screenshotText, workerVersion }):
  task = load(taskId)
  require task.status in (PENDING, RUNNING)
  task.status = SUCCEEDED
  task.capturedAt = capturedAt
  task.html = html
  task.screenshotText = screenshotText
  save(task)

fail(taskId, { failedAt, errorType, message, workerVersion }):
  task = load(taskId)
  require task.status in (PENDING, RUNNING)
  task.status = FAILED
  task.failedAt = failedAt
  task.failureType = errorType
  task.failureMessage = message
  save(task)
```

---

## 七、代码文件锚点（只用于对照，不要求先通读）


| 职责                                    | 文件                                                      |
| ------------------------------------- | ------------------------------------------------------- |
| Worker 编排名、调 create/start/finish/fail | `tracenest-worker/src/app.js`                           |
| 发 HTTP、`{ url }` / body 拼装            | `tracenest-worker/src/uploadClient.js`                  |
| 占位抓取、返回 `html`/`screenshotText`       | `tracenest-worker/src/captureService.js`                |
| 创建任务、查询任务                             | `tracenest-be/.../CaptureTaskController.java`           |
| start/finish/fail 回调与状态               | `tracenest-be/.../WorkerCaptureCallbackController.java` |
| 任务字段内存存取                              | `tracenest-be/.../InMemoryCaptureTaskRepository.java`   |


---

## 八、下一步（证据包 Step2）

真实 `png`、每文件 `sha256/manifest`、finish body 升级等：**写在单独规划里**，避免和本条数据流混在一起导致再一次读乱。见同目录下 `step2-evidence-design.md`（现为极简入口说明）。