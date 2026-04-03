# Step2：证据包（极简入口）

**本条不再展开长说明**，避免与 Step1 数据流混在一起。

请先完整阅读：`first-step-dev-plan.md`（Step1：`create → start → finish/fail` 的变量、URL、Body、状态机伪码）。

Step2 方向（你实现完 Step1 后再做）：

- Worker：真实 `page.html` + `screenshot.png`（或等价产物）
- 对每个 artifact 计算 `sha256`、`size`
- `finish` 请求体逐步改为携带 `manifest`（或 artifact 元信息），后端持久化/内存保存后再做验签

具体字段与验收清单由你迭代时自行补充；需要模板时可单独开文档，不必回本文件堆长文。
