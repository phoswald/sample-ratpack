package com.github.phoswald.sample.task;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.github.phoswald.sample.task.TaskEntity;
import com.github.phoswald.sample.task.TaskRepository;
import com.github.phoswald.sample.task.TaskRepository.Transaction;

class TaskRepositoryTest {

    private final TaskRepository testee = new TaskRepository(createConnection());

    @AfterEach
    void cleanup() {
        testee.close();
    }

    @Test
    void testCrud() {
        try(Transaction txn = testee.openTransaction()) {
            assertEquals(0, testee.selectAllTasks().size());

            TaskEntity entity = new TaskEntity();
            entity.setNewTaskId();
            entity.setTitle("Test Title");
            entity.setDescription("Test Description");
            testee.createTask(entity);
        }
        try(Transaction txn = testee.openTransaction()) {
            List<TaskEntity> entites = testee.selectAllTasks();

            assertEquals(1, entites.size());
            assertEquals("Test Title", entites.get(0).getTitle());
            assertEquals("Test Description", entites.get(0).getDescription());
        }
    }

    private Connection createConnection() {
        try {
            return DriverManager.getConnection("jdbc:h2:mem:test;INIT=RUNSCRIPT FROM 'src/main/resources/schema.sql'", "sa", "sa");
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
