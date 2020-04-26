package com.github.phoswald.sample.ratpack.task;

import com.github.phoswald.sample.ratpack.AbstractView;

public class TaskEditView extends AbstractView<TaskViewModel> {

    public TaskEditView() {
        super("task-edit", "task");
    }
}
