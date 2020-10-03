package com.github.phoswald.sample.task;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

public class TaskEntity {

    private String taskId;

    private String userId;

    private Date timestamp;

    private String title;

    private String description;

    private boolean done;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public void setNewTaskId() {
        this.taskId = UUID.randomUUID().toString();
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Instant getTimestamp() {
        return timestamp == null ? null : timestamp.toInstant();
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp == null ? null : Date.from(timestamp);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }
}
