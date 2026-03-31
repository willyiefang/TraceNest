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

    private Instant startedAt;
    private Instant failedAt;
    private String failureType;
    private String failureMessage;

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
    //v2:success
    public Instant getCapturedAt() {
        return capturedAt;
    }

    public void setCapturedAt(Instant capturedAt) {
        this.capturedAt = capturedAt;
    }
    //v1:start
    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }
    //v3:fail
    public Instant getFailedAt() {
        return failedAt;
    }

    public void setFailedAt(Instant failedAt) {
        this.failedAt = failedAt;
    }   
    //FAILED
    public String getFailureType() {
        return failureType;
    }

    public void setFailureType(String failureType) {
        this.failureType = failureType;
    }
    //FAILED details:
    public String getFailureMessage() {
        return failureMessage;
    }

    public void setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
    }
}
