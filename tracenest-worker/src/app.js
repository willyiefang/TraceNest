import { runCapture } from "./captureService.js";
import { createCaptureTask, finishCaptureTask, checkApiHealth } from "./uploadClient.js";

async function main() {
  const targetUrl = process.env.TARGET_URL || "https://example.com";
  let backendTaskId = process.env.TASK_ID; // if not provided, worker will create it via API

  if (!backendTaskId) {
    console.log(`[worker] creating backend task for url=${targetUrl}`);
    const created = await createCaptureTask(targetUrl);
    backendTaskId = created.taskId;
  }

  console.log(`[worker] start url=${targetUrl} taskId=${backendTaskId}`);
  const result = await runCapture(backendTaskId, targetUrl);

  // health check for easier debugging
  try {
    const health = await checkApiHealth();
    console.log("[worker] api reachable", health);
  } catch (e) {
    console.warn("[worker] api not reachable:", e.message);
  }

  const finishPayload = {
    capturedAt: result.capturedAt,
    html: result.html,
    screenshotText: result.screenshotText,
    workerVersion: "worker-v0",
  };

  const finished = await finishCaptureTask(backendTaskId, finishPayload);
  console.log("[worker] finished task", finished);
}

main().catch((error) => {
  console.error("[worker] failed", error);
  process.exit(1);
});
