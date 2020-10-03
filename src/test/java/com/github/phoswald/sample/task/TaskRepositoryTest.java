package com.github.phoswald.sample.task;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.jupiter.api.Test;

import com.github.phoswald.sample.task.TaskEntity;
import com.github.phoswald.sample.task.TaskRepository;
import com.github.phoswald.sample.task.TaskRepository.Transaction;

class TaskRepositoryTest {

    private static final EntityManagerFactory emf = Persistence.createEntityManagerFactory("taskDS",
            Collections.singletonMap("javax.persistence.jdbc.url", "jdbc:h2:mem:test"));

    private final TaskRepository testee = new TaskRepository(emf.createEntityManager());

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
}
