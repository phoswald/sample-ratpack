package com.github.phoswald.sample.ratpack.task;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

class TaskListViewTest {

    private final TaskListView testee = new TaskListView();

    @Test
    void testRendering() {
        List<TaskViewModel> model = createModel();
        String html = testee.render(model);
        assertTrue(html.startsWith("<!doctype html>"));
        assertTrue(html.endsWith("</html>\n"));
        assertTrue(html.contains("Tasks Overview"));
        assertTrue(html.contains("TestTitle"));
    }

    private List<TaskViewModel> createModel() {
        TaskEntity entity = new TaskEntity();
        entity.setNewTaskId();
        entity.setTitle("TestTitle");
        entity.setTimestamp(Instant.now());
        return Collections.singletonList(new TaskViewModel(entity));
    }
}
