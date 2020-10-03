package com.github.phoswald.sample.task;

import static com.github.phoswald.sample.jooq.Tables.TASK;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

public class TaskRepository implements AutoCloseable {

    private final Connection conn;
    private final DSLContext dsl;

    public TaskRepository(Connection conn) {
        this.conn = conn;
        this.dsl = DSL.using(conn, SQLDialect.H2);
    }

    public Transaction openTransaction() {
        // TODO: create transaction? or implicit?
        return new Transaction() {
            @Override
            public void close() {
                // TODO: commit transaction? or implicit?
            }
        };
    }

    @Override
    public void close() {
        try {
            conn.close();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public List<TaskEntity> selectAllTasks() {
        Result<Record> result = dsl.select().from(TASK).orderBy(TASK.TIMESTAMP.desc()).fetch();
        List<TaskEntity> entities = new ArrayList<>();
        for(Record record : result) {
            TaskEntity entity = new TaskEntity();
            entity.setTaskId(record.get(TASK.TASK_ID));
            entity.setUserId(record.get(TASK.USER_ID));
            entity.setTimestamp(convertTimestamp(record.get(TASK.TIMESTAMP)));
            entity.setTitle(record.get(TASK.TITLE));
            entity.setDescription(record.get(TASK.DESCRIPTION));
            entity.setDone(record.get(TASK.DONE));
            entities.add(entity);
        }
        return entities;
    }

    public TaskEntity selectTaskById(String taskId) {
        Record record = dsl.select().from(TASK).where(TASK.TASK_ID.eq(taskId)).fetchSingle();
        TaskEntity entity = new TaskEntity();
        entity.setTaskId(record.get(TASK.TASK_ID));
        entity.setUserId(record.get(TASK.USER_ID));
        entity.setTimestamp(convertTimestamp(record.get(TASK.TIMESTAMP)));
        entity.setTitle(record.get(TASK.TITLE));
        entity.setDescription(record.get(TASK.DESCRIPTION));
        entity.setDone(record.get(TASK.DONE));
        return entity;
    }

    public void createTask(TaskEntity entity) {
        dsl.insertInto(TASK, TASK.TASK_ID, TASK.USER_ID, TASK.TIMESTAMP, TASK.TITLE, TASK.DESCRIPTION, TASK.DONE)
            .values(entity.getTaskId(), entity.getUserId(), convertTimestamp(entity.getTimestamp()), entity.getTitle(), entity.getDescription(), entity.isDone())
            .execute();
    }

    public void deleteTask(TaskEntity entity) {
        dsl.deleteFrom(TASK).where(TASK.TASK_ID.eq(entity.getTaskId())).execute();
    }

    public void updateTask(TaskEntity entity) {
        dsl.update(TASK)
                .set(TASK.TIMESTAMP, convertTimestamp(entity.getTimestamp()))
                .set(TASK.TITLE, entity.getTitle())
                .set(TASK.DESCRIPTION, entity.getDescription())
                .set(TASK.DONE, entity.isDone())
                .where(TASK.TASK_ID.eq(entity.getTaskId()))
                .execute();
    }

    private Instant convertTimestamp(LocalDateTime t) {
        return t == null ? null : t.toInstant(ZoneOffset.UTC);
    }

    private LocalDateTime convertTimestamp(Instant t) {
        return t == null ? null : t.atOffset(ZoneOffset.UTC).toLocalDateTime();
    }

    public static interface Transaction extends AutoCloseable {
        @Override
        public void close();
    }
}
