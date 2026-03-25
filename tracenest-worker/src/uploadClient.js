export async function reportCaptureResult(result) {
  const baseUrl = process.env.API_BASE_URL || "http://localhost:8080";
  const endpoint = `${baseUrl}/api/v1/health`;

  try {
    const response = await fetch(endpoint);
    const data = await response.json();
    console.log("[worker] api reachable", data);
  } catch (error) {
    console.warn("[worker] api not reachable, skip callback");
  }

  console.log("[worker] capture result", result);
}
