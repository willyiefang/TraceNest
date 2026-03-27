package com.tracenest.api.capture;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CaptureTaskController {

    private final InMemoryCaptureTaskRepository repository;

    public CaptureTaskController(InMemoryCaptureTaskRepository repository) {
        this.repository = repository;
    }

    public record CreateCaptureTaskRequest(@NotBlank String url) {}

    public record CreateCaptureTaskResponse(String taskId, CaptureTaskStatus status) {}

    public record CaptureTaskResponse(
            String taskId,
            String url,
            CaptureTaskStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record FinishCaptureTaskRequest(
            Instant capturedAt,
            @NotBlank String html,
            @NotBlank String screenshotText,
            String workerVersion
    ) {}

    @PostMapping("/capture-tasks")
    public CreateCaptureTaskResponse createCaptureTask(@Valid @RequestBody CreateCaptureTaskRequest request) {
        String taskId = java.util.UUID.randomUUID().toString();
        CaptureTask task = repository.create(taskId, request.url());
        return new CreateCaptureTaskResponse(task.getTaskId(), task.getStatus());
    }

    @GetMapping("/capture-tasks/{taskId}")
    public CaptureTaskResponse getCaptureTask(@PathVariable String taskId) {
        CaptureTask task = repository.findById(taskId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "task not found"));

        return new CaptureTaskResponse(
                task.getTaskId(),
                task.getUrl(),
                task.getStatus(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
