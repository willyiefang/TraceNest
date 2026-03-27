package com.tracenest.api.capture;

import java.time.Instant;

public class CaptureTask {

    private String taskId;
    private String url;
    private CaptureTaskStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    // v0: store artifact contents in memory (later: object storage)
    private String html;
    private String screenshotText;
    private Instant capturedAt;

    public CaptureTask() {}

    public CaptureTask(String taskId, String url, CaptureTaskStatus status, Instant createdAt) {
        this.taskId = taskId;
        this.url = url;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public CaptureTaskStatus getStatus() {
        return status;
    }

    public void setStatus(CaptureTaskStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    public String getScreenshotText() {
        return screenshotText;
    }

    public void setScreenshotText(String screenshotText) {
        this.screenshotText = screenshotText;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }

    public void setCapturedAt(Instant capturedAt) {
        this.capturedAt = capturedAt;
    }
}
