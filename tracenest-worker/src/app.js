import { runCapture } from "./captureService.js";
import { reportCaptureResult } from "./uploadClient.js";

async function main() {
  const targetUrl = process.env.TARGET_URL || "https://example.com";
  const taskId = process.env.TASK_ID || "local-dev-task-1";

  console.log(`[worker] start task=${taskId} url=${targetUrl}`);
  const result = await runCapture(taskId, targetUrl);
  await reportCaptureResult(result);
  console.log("[worker] done");
}

main().catch((error) => {
  console.error("[worker] failed", error);
  process.exit(1);
});
