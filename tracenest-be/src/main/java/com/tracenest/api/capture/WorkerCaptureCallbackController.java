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

    public record StartCaptureTaskRequest(
            Instant startedAt,
            String workerVersion
    ) {}

    public record FailCaptureTaskRequest(
            Instant failedAt,
            @NotBlank String errorType,
            @NotBlank String message,
            String workerVersion
    ) {}

    private void assertTransition(CaptureTaskStatus current, CaptureTaskStatus expectedFrom, String error) {
        if (current != expectedFrom) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.CONFLICT, error);
        }
    }

    public record CaptureTaskResponse(
            String taskId,
            String url,
            CaptureTaskStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {}

    @PostMapping("/{taskId}/start")
    public CaptureTaskResponse startTask(
            @PathVariable String taskId,
            @Valid @RequestBody StartCaptureTaskRequest request
    ){
        CaptureTask task = repository.findById(taskId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "task not found"));

        // Only allow start from PENDING.
        assertTransition(task.getStatus(), CaptureTaskStatus.PENDING, "task cannot be started from current status");

        task.setStatus(CaptureTaskStatus.RUNNING);
        task.setStartedAt(request.startedAt());
        repository.save(task);

        return new CaptureTaskResponse(
                task.getTaskId(),
                task.getUrl(),
                task.getStatus(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
    @PostMapping("/{taskId}/fail")
    public CaptureTaskResponse failTask(
            @PathVariable String taskId,
            @Valid @RequestBody FailCaptureTaskRequest request
    ){
        CaptureTask task = repository.findById(taskId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "task not found"));

        // Allow fail from PENDING or RUNNING.
        CaptureTaskStatus current = task.getStatus();
        if (current != CaptureTaskStatus.PENDING && current != CaptureTaskStatus.RUNNING) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.CONFLICT, "task cannot be failed from current status");
        }

        task.setStatus(CaptureTaskStatus.FAILED);
        task.setFailedAt(request.failedAt());
        task.setFailureType(request.errorType());
        task.setFailureMessage(request.message());
        repository.save(task);

        return new CaptureTaskResponse(
                task.getTaskId(),
                task.getUrl(),
                task.getStatus(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
    
    @PostMapping("/{taskId}/finish")
    public CaptureTaskResponse finishTask(
            @PathVariable String taskId,
            @Valid @RequestBody FinishCaptureTaskRequest request
    ) {
        CaptureTask task = repository.findById(taskId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "task not found"));

        // Allow finish from PENDING or RUNNING (worker may skip explicit start in early versions).
        CaptureTaskStatus current = task.getStatus();
        if (current != CaptureTaskStatus.PENDING && current != CaptureTaskStatus.RUNNING) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.CONFLICT, "task cannot be finished from current status");
        }

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
