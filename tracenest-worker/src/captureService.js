import fs from "node:fs/promises";
import path from "node:path";

export async function runCapture(taskId, url) {
  if (process.env.FORCE_FAIL === "1") {
    throw new Error("forced failure for testing");
  }

  const outputDir = path.resolve("output", taskId);
  await fs.mkdir(outputDir, { recursive: true });

  const capturedAt = new Date().toISOString();
  const htmlPath = path.join(outputDir, "page.html");
  const screenshotPath = path.join(outputDir, "screenshot.txt");

  // Placeholder capture output until browser integration is added.
  const html = `<html><body><h1>Captured</h1><p>url=${url}</p><p>capturedAt=${capturedAt}</p></body></html>`;
  const screenshotText = `fake-screenshot for ${url} at ${capturedAt}\n`;
  await fs.writeFile(htmlPath, html, "utf-8");
  await fs.writeFile(screenshotPath, screenshotText, "utf-8");

  return {
    taskId,
    url,
    status: "SUCCEEDED",
    capturedAt,
    htmlPath,
    screenshotPath,
    // Return artifact contents so the worker can POST them to the API.
    html,
    screenshotText,
  };
}
