package com.tracenest.api.capture;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

@Repository
public class InMemoryCaptureTaskRepository {

    private final Map<String, CaptureTask> store = new ConcurrentHashMap<>();

    public CaptureTask create(String taskId, String url) {
        Instant now = Instant.now();
        CaptureTask task = new CaptureTask(taskId, url, CaptureTaskStatus.PENDING, now);
        task.setUpdatedAt(now);
        store.put(taskId, task);
        return task;
    }

    public Optional<CaptureTask> findById(String taskId) {
        return Optional.ofNullable(store.get(taskId));
    }

    public CaptureTask save(CaptureTask task) {
        task.setUpdatedAt(Instant.now());
        store.put(task.getTaskId(), task);
        return task;
    }
}
