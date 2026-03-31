# Step 2 设计文档：从状态机到证据对象

> 目标：在已完成 `create -> start -> finish/fail` 状态机的基础上，把占位产物升级为可验证证据的前半段（artifact + hash + manifest 草稿）。

---

## 1. 当前基线（Step 1 已完成）

已具备：

- 任务状态机最小闭环：`PENDING -> RUNNING -> SUCCEEDED/FAILED`
- Worker 与 API 的回调链路：
  - `POST /api/v1/capture-tasks`
  - `POST /internal/v1/worker/capture-tasks/{taskId}/start`
  - `POST /internal/v1/worker/capture-tasks/{taskId}/finish`
  - `POST /internal/v1/worker/capture-tasks/{taskId}/fail`

当前限制：

- 抓取产物是占位内容（fake html/txt）
- finish 回调偏“文本传输”，不适合后续对象存储和可验证证据包

---

## 2. Step 2 核心目标

Step 2 不做签名，只做“可验证证据结构的前半段”：

1. Worker 产出真实 artifact（至少 html + screenshot png）
2. Worker 计算 artifact 的 `sha256` 和 `size`
3. finish 回调改为上传“证据元信息”
4. 后端保存证据元信息并可查询
5. 生成最小 manifest（可先作为 JSON 字段，不必独立文件）

---

## 3. 证据对象最小模型

### 3.1 ArtifactItem（单个证据文件元信息）

- `name`: 文件逻辑名（例如 `page.html`, `screenshot.png`）
- `path`: 本地路径或存储路径（后续接 MinIO 时替换为对象路径）
- `mimeType`: `text/html` / `image/png`
- `size`: 文件字节数
- `sha256`: 文件内容哈希

### 3.2 EvidenceManifest（最小清单）

- `schemaVersion`: 例如 `v1`
- `packageId`: 先用 `taskId` 或随机 UUID
- `taskId`
- `sourceUrl`
- `capturedAt`
- `workerVersion`
- `artifacts`: `ArtifactItem[]`

---

## 4. API 设计（Step 2）

## 4.1 finish 请求体（建议替代现有大文本字段）

```json
{
  "capturedAt": "2026-03-27T03:01:29.082Z",
  "workerVersion": "worker-v1",
  "manifest": {
    "schemaVersion": "v1",
    "packageId": "pkg-uuid",
    "taskId": "task-uuid",
    "sourceUrl": "https://example.com",
    "capturedAt": "2026-03-27T03:01:29.082Z",
    "workerVersion": "worker-v1",
    "artifacts": [
      {
        "name": "page.html",
        "path": "output/task-uuid/page.html",
        "mimeType": "text/html",
        "size": 12345,
        "sha256": "..."
      },
      {
        "name": "screenshot.png",
        "path": "output/task-uuid/screenshot.png",
        "mimeType": "image/png",
        "size": 45678,
        "sha256": "..."
      }
    ]
  }
}
```

> 说明：先保留现有字段兼容也可以，但建议逐步迁移到 `manifest` 主体。

---

## 5. 代码改造顺序（建议按这个顺序做）

### 5.1 Worker：先产真实 artifact

目标：

- 把 `captureService` 从占位字符串替换为真实抓取结果
- 输出 `page.html` + `screenshot.png`

建议：

- 引入 Playwright
- 统一输出目录：`output/<taskId>/`

### 5.2 Worker：计算 hash 和 size

目标：

- 对每个 artifact 计算 `sha256` 与 `size`
- 组装 `manifest` 对象

建议：

- 把 hash 逻辑独立为 util（便于后续复用到 verify）

### 5.3 Worker -> API：finish 传 manifest

目标：

- `finishCaptureTask` body 传 `manifest`
- 失败路径 `fail` 保持不变（errorType/message）

### 5.4 API：保存证据元信息

目标：

- 在 `CaptureTask` 增加 `manifest`（先字符串或对象都可）
- finish 时把 manifest 保存到任务

建议：

- Step 2 仍可内存存储
- Step 3 再迁移到数据库/对象存储

---

## 6. 验收标准（Definition of Done）

满足以下全部条件即 Step 2 完成：

- [ ] Worker 运行后生成真实 `page.html` 和 `screenshot.png`
- [ ] finish 请求体包含 `manifest.artifacts[*].sha256/size`
- [ ] API 返回任务时可看到 manifest 摘要
- [ ] 手动篡改 artifact 后，重新计算 hash 与 manifest 不一致（证明可校验）

---

## 7. 常见坑与规避

### 7.1 路由前缀冲突

- 问题：`/api/v1` 与 `/internal/v1` 混用时易拼错
- 规避：external controller 与 internal callback controller 分开

### 7.2 finish 请求体过大

- 问题：直接传大 html 内容会膨胀
- 规避：Step 2 起以 artifact 元信息为主，内容文件由 Worker 本地/对象存储保管

### 7.3 状态乱序

- 问题：`finish` 在 `start` 前到达
- 规避：Controller 做状态校验（只允许合法迁移）

---

## 8. Step 3 预告（下一阶段）

Step 3 才进入“可验证证据包的后半段”：

- 对 manifest 做签名（如 Ed25519）
- 提供 verify 接口（验 hash + 验签）
- 引入持久化与对象存储（MySQL + MinIO）

---

## 9. 从设计到实现（零基础直白版）

本节只回答一个问题：**设计图怎么一步步变成代码**。

### 9.1 先记住 3 个对象

1. `task`：任务本身（状态是 PENDING/RUNNING/SUCCEEDED/FAILED）
2. `artifact`：任务产出的文件（html、png 等）
3. `manifest`：artifact 清单 + 每个文件的 hash（用于后续校验）

---

### 9.2 设计先落在“字段”，不要先写复杂代码

你先把这几个字段写到纸上/文档里，确认名字和意义：

- `taskId`：任务编号
- `sourceUrl`：抓取地址
- `capturedAt`：抓取时间
- `artifacts[]`：
  - `name`
  - `path`
  - `size`
  - `sha256`

只要字段定了，实现就不会乱。

---

### 9.3 再落到代码（按文件分工）

#### A. `captureService.js`（只负责“生成文件”）

位置：`tracenest-worker/src/captureService.js`

你要做的：

- 生成 `page.html`
- 生成 `screenshot.png`（Step 2 目标，替代 txt）

一句话：**这里不管接口，只管产物文件。**

#### B. `hashUtil.js`（只负责“算指纹”）

建议新增：`tracenest-worker/src/hashUtil.js`

你要做的：

- 输入文件路径
- 输出 `sha256` + `size`

一句话：**这里不管业务状态，只管文件校验信息。**

#### C. `app.js`（只负责“串流程 + 组请求体”）

位置：`tracenest-worker/src/app.js`

你要做的：

- 创建任务（create）
- 开始任务（start）
- 调用 captureService 拿产物路径
- 调用 hashUtil 得到 artifacts 元信息
- 组装 `manifest`
- finish 回调给后端

一句话：**这里是总导演。**

#### D. `uploadClient.js`（只负责“发 HTTP”）

位置：`tracenest-worker/src/uploadClient.js`

你要做的：

- `create/start/finish/fail` 发请求
- finish 的 body 换成 `manifest` 结构

一句话：**这里不做计算，只做请求发送。**

#### E. `WorkerCaptureCallbackController.java`（后端接收 finish）

位置：`tracenest-be/src/main/java/com/tracenest/api/capture/WorkerCaptureCallbackController.java`

你要做的：

- 把 finish 请求体从 `html/screenshotText` 升级为包含 `manifest`
- 存到 `CaptureTask`（先内存存）

一句话：**这里是后端入口。**

#### F. `CaptureTask.java`（任务数据模型）

位置：`tracenest-be/src/main/java/com/tracenest/api/capture/CaptureTask.java`

你要做的（最简）：

- 新增 `manifestJson` 字段（字符串存 JSON）

一句话：**这里是后端存储结构。**

---

### 9.4 一步一步验收（每步都能看到结果）

1. 跑 Worker 后确认目录有：
   - `page.html`
   - `screenshot.png`
2. 打印 finish payload，确认有：
   - `manifest.artifacts[*].sha256`
   - `manifest.artifacts[*].size`
3. 调 `GET /api/v1/capture-tasks/{taskId}`，确认任务已 `SUCCEEDED`
4. 手动改动一个 artifact，再算 hash，应与 manifest 不一致

---

### 9.5 新手常见卡点

- **卡点 1：到底该改哪个文件**
  - 记口诀：`生成文件 -> 算 hash -> 组 manifest -> 发 finish -> 后端保存`
- **卡点 2：请求体看不懂**
  - 看 `uploadClient.js` 里 `fetch(... body: JSON.stringify(...))`
- **卡点 3：状态机和证据逻辑混在一起**
  - 状态机在 controller 里转状态；证据逻辑在 worker 里组数据

---

### 9.6 本阶段只要做到这 6 条就算成功

- [ ] 真实 artifact 生成（html + png）
- [ ] artifact 的 `sha256/size` 可计算
- [ ] finish body 含 manifest
- [ ] 后端可接收并保存 manifest
- [ ] 任务状态链路仍可跑通
- [ ] 篡改文件后 hash 校验失败（证明设计方向正确）

