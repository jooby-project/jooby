/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3936;

import java.util.List;
import java.util.Map;

import io.jooby.ModelAndView;
import io.jooby.annotation.*;
import io.jooby.annotation.htmx.HxError;
import io.jooby.annotation.htmx.HxOob;
import io.jooby.annotation.htmx.HxTrigger;
import io.jooby.annotation.htmx.HxView;
import io.jooby.htmx.HtmxResponse;
import jakarta.validation.Valid;

@HxError("task_error.hbs")
public class TaskUI {
  private final TaskRepo3936 db;

  public TaskUI(TaskRepo3936 db) {
    this.db = db;
  }

  @GET("/")
  public ModelAndView<TaskBoard3936> index() {
    return new ModelAndView<>("index.hbs", getBoard());
  }

  // 1. Load the initial board
  @GET("/tasks")
  @HxView(value = "board.hbs")
  public TaskBoard3936 getBoard() {
    return db.getBoardState();
  }

  // 2. Add a task and update the counter simultaneously
  @POST("/tasks")
  @HxView(value = "task_row.hbs")
  @HxOob("task_counter.hbs")
  @HxOob("toast.hbs")
  @HxTrigger("taskAdded")
  public Map<String, Object> addTask(@FormParam @Valid TaskDto3936 dto) {
    var newTask = db.save(dto);
    return Map.of(
        "id",
        newTask.id(),
        "title",
        newTask.title(),
        "completed",
        newTask.completed(),
        "activeCount",
        db.getActiveCount(),
        "message",
        "Task added successfully!");
  }

  // 3. Delete a task (Returns nothing, but triggers the OOB counter)
  @DELETE("/tasks/{id}")
  public HtmxResponse deleteTask(@PathParam String id) {
    db.delete(id);
    return HtmxResponse.empty()
        .addOob("task_counter.hbs", Map.of("activeCount", db.getActiveCount()))
        .addOob("toast.hbs", Map.of("message", "Task deleted!"));
  }

  // 4. Save the new Drag-and-Drop order
  @POST("/tasks/reorder")
  public HtmxResponse reorderTasks(@FormParam List<String> taskIds) {
    db.updateOrder(taskIds);
    return HtmxResponse.empty().addOob("toast.hbs", Map.of("message", "Board saved."));
  }
}
