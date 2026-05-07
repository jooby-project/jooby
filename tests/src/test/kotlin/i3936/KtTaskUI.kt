/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3936

import io.jooby.ModelAndView
import io.jooby.annotation.*
import io.jooby.annotation.htmx.HxError
import io.jooby.annotation.htmx.HxOob
import io.jooby.annotation.htmx.HxTrigger
import io.jooby.annotation.htmx.HxView
import io.jooby.htmx.HtmxResponse
import jakarta.validation.Valid

class KtTaskUI(private val db: TaskRepo3936) {
  @GET("/")
  fun index(): ModelAndView<TaskBoard3936> {
    return ModelAndView("index.hbs", getBoard())
  }

  @HxView(value = "board.hbs")
  @GET("/tasks")
  fun getBoard(): TaskBoard3936 {
    // 1. Load the initial board
    val taskBoard3936 = TaskBoard3936(4, listOf<Task3936>())
    return taskBoard3936
  }

  // 2. Add a task and update the counter simultaneously
  @POST("/tasks")
  @HxView(value = "task_row.hbs")
  @HxOob("task_counter.hbs")
  @HxOob("toast.hbs")
  @HxTrigger("taskAdded")
  @HxError("task_error.hbs")
  fun addTask(@FormParam @Valid dto: @Valid TaskDto3936?): Map<String, Any> {
    val newTask = db.save(dto)
    return mapOf(
      "id" to newTask.id,
      "title" to newTask.title,
      "completed" to newTask.completed,
      "activeCount" to db.getActiveCount(),
      "message" to "Task added successfully!",
    )
  }

  // 3. Delete a task (Returns nothing, but triggers the OOB counter)
  @DELETE("/tasks/{id}")
  fun deleteTask(@PathParam id: String?): HtmxResponse {
    db.delete(id)
    return HtmxResponse.empty()
      .addOob("task_counter.hbs", mapOf("activeCount" to db.getActiveCount()))
      .addOob("toast.hbs", mapOf("message" to "Task deleted!"))
  }

  // 4. Save the new Drag-and-Drop order
  @POST("/tasks/reorder")
  fun reorderTasks(@FormParam taskIds: MutableList<String?>?): HtmxResponse {
    db.updateOrder(taskIds)
    return HtmxResponse.empty().addOob("toast.hbs", mapOf("message" to "Board saved."))
  }
}
