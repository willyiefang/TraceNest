package com.tracenest.api.capture;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/worker/capture-tasks")
public class WorkerCaptureCallbackController {

    private final InMemoryCaptureTaskRepository repository;

    public WorkerCaptureCallbackController(InMemoryCaptureTaskRepository repository) {
        this.repository = repository;
    }

    public record FinishCaptureTaskRequest(
            Instant capturedAt,
            @NotBlank String html,
            @NotBlank String screenshotText,
            String workerVersion
    ) {}

    public record CaptureTaskResponse(
            String taskId,
            String url,
            CaptureTaskStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {}

    @PostMapping("/{taskId}/finish")
    public CaptureTaskResponse finishTask(
            @PathVariable String taskId,
            @Valid @RequestBody FinishCaptureTaskRequest request
    ) {
        CaptureTask task = repository.findById(taskId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "task not found"));

        task.setStatus(CaptureTaskStatus.SUCCEEDED);
        task.setCapturedAt(request.capturedAt());
        task.setHtml(request.html());
        task.setScreenshotText(request.screenshotText());
        repository.save(task);

        return new CaptureTaskResponse(
                task.getTaskId(),
                task.getUrl(),
                task.getStatus(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
