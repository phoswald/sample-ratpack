package com.github.phoswald.sample.ratpack.task;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;

public class TaskRepository implements AutoCloseable {

    private static final EntityManagerFactory emf = Persistence.createEntityManagerFactory("taskDS");

    private final EntityManager em;

    private TaskRepository(boolean txn) {
        em = emf.createEntityManager();
        if(txn) {
            em.getTransaction().begin();
        }
    }

    public static TaskRepository openReadOnly() {
        return new TaskRepository(false);
    }

    public static TaskRepository openReadWrite() {
        return new TaskRepository(true);
    }

    @Override
    public void close() {
        try {
            if(em.getTransaction().isActive()) {
                em.getTransaction().commit();
            }
        } finally {
            em.close();
        }
    }

    public List<TaskEntity> selectAllTasks() {
        TypedQuery<TaskEntity> query = em.createNamedQuery(TaskEntity.SELECT_ALL, TaskEntity.class);
        query.setMaxResults(100);
        return query.getResultList();
    }

    public TaskEntity selectTaskById(String taskId) {
        return em.find(TaskEntity.class, taskId);
    }

    public void createTask(TaskEntity entity) {
        em.persist(entity);
    }

    public void deleteTask(TaskEntity entity) {
        em.remove(entity);
    }

    public void updateChanges() {
        em.flush();
    }
}
