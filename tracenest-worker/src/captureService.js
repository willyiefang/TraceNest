import fs from "node:fs/promises";
import path from "node:path";

export async function runCapture(taskId, url) {
  const outputDir = path.resolve("output", taskId);
  await fs.mkdir(outputDir, { recursive: true });

  const capturedAt = new Date().toISOString();
  const htmlPath = path.join(outputDir, "page.html");
  const screenshotPath = path.join(outputDir, "screenshot.txt");

  // Placeholder capture output until browser integration is added.
  const html = `<html><body><h1>Captured</h1><p>url=${url}</p><p>capturedAt=${capturedAt}</p></body></html>`;
  await fs.writeFile(htmlPath, html, "utf-8");
  await fs.writeFile(
    screenshotPath,
    `fake-screenshot for ${url} at ${capturedAt}\n`,
    "utf-8"
  );

  return {
    taskId,
    url,
    status: "SUCCEEDED",
    capturedAt,
    htmlPath,
    screenshotPath,
  };
}
