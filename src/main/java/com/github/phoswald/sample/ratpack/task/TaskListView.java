package com.github.phoswald.sample.ratpack.task;

import java.util.List;

import com.github.phoswald.sample.ratpack.AbstractView;

public class TaskListView extends AbstractView<List<TaskViewModel>> {

    public TaskListView() {
        super("task-list", "tasks");
    }
}
