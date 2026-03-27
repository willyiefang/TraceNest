export async function reportCaptureResult(result) {
  // Backward-compatible shim: keep the function name,
  // but now it will actually create -> finish a capture task.
  await createAndFinishCaptureTask(result.url, result.taskId, result);
}

function apiBaseUrl() {
  return process.env.API_BASE_URL || "http://localhost:8080";
}

export async function checkApiHealth() {
  const endpoint = `${apiBaseUrl()}/api/v1/health`;
  const response = await fetch(endpoint, { method: "GET" });
  if (!response.ok) throw new Error(`health check failed: ${response.status}`);
  return response.json();
}

export async function createCaptureTask(url) {
  const endpoint = `${apiBaseUrl()}/api/v1/capture-tasks`;
  const response = await fetch(endpoint, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ url }),
  });
  if (!response.ok) {
    const text = await response.text().catch(() => "");
    throw new Error(`create task failed: ${response.status} ${text}`);
  }
  return response.json();
}

export async function finishCaptureTask(taskId, payload) {
  const endpoint = `${apiBaseUrl()}/internal/v1/worker/capture-tasks/${encodeURIComponent(
    taskId
  )}/finish`;

  const response = await fetch(endpoint, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const text = await response.text().catch(() => "");
    throw new Error(`finish task failed: ${response.status} ${text}`);
  }

  return response.json();
}

async function createAndFinishCaptureTask(url, existingTaskId, result) {
  // This worker runs in a "demo" mode for v0:
  // 1) create capture task (or reuse an id if provided)
  // 2) capture artifacts locally
  // 3) POST finish callback to the API with artifact contents

  // (1) create task (if taskId wasn't provided)
  let taskId = existingTaskId;
  if (!taskId) {
    const created = await createCaptureTask(url);
    taskId = created.taskId;
  } else {
    // For now, if taskId is provided, we still finish it.
    // (In the next iteration we can add "get task" + "start" semantics.)
  }

  // (2) finish callback
  const finishPayload = {
    capturedAt: result.capturedAt,
    html: result.html,
    screenshotText: result.screenshotText,
    workerVersion: "worker-v0",
  };

  // optional health check to keep the old "連通性提示" behaviour
  try {
    const health = await checkApiHealth();
    console.log("[worker] api reachable", health);
  } catch (e) {
    console.warn("[worker] api not reachable (finish may fail):", e.message);
  }

  const finished = await finishCaptureTask(taskId, finishPayload);
  console.log("[worker] finished task", finished);
}
