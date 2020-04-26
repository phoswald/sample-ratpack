package com.github.phoswald.sample.ratpack.task;

import java.time.Instant;
import java.util.List;

public class TaskResource {

    public List<TaskEntity> getTasks() {
        try(TaskRepository repository = TaskRepository.openReadOnly()) {
            List<TaskEntity> entities = repository.selectAllTasks();
            return entities;
        }
    }

    public TaskEntity postTasks(TaskEntity request) {
        try(TaskRepository repository = TaskRepository.openReadWrite()) {
            TaskEntity entity = new TaskEntity();
            entity.setNewTaskId();
            entity.setUserId("guest");
            entity.setTimestamp(Instant.now());
            entity.setTitle(request.getTitle());
            entity.setDescription(request.getDescription());
            entity.setDone(request.isDone());
            repository.createTask(entity);
            return entity;
        }
    }

    public TaskEntity getTask(String id) {
        try(TaskRepository repository = TaskRepository.openReadOnly()) {
            TaskEntity entity = repository.selectTaskById(id);
            return entity;
        }
    }

    public TaskEntity putTask(String id, TaskEntity request) {
        try(TaskRepository repository = TaskRepository.openReadWrite()) {
            TaskEntity entity = repository.selectTaskById(id);
            entity.setTimestamp(Instant.now());
            entity.setTitle(request.getTitle());
            entity.setDescription(request.getDescription());
            entity.setDone(request.isDone());
            return entity;
        }
    }

    public Void deleteTask(String id) {
        try(TaskRepository repository = TaskRepository.openReadWrite()) {
            TaskEntity entity = repository.selectTaskById(id);
            repository.deleteTask(entity);
            return null;
        }
    }
}
