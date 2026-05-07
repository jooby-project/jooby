/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3936;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskRepo3936 {
  private final AtomicInteger errors = new AtomicInteger(0);
  private final Map<String, Task3936> db = new ConcurrentHashMap<>();
  private final AtomicInteger idGen = new AtomicInteger(1);
  // Stores the physical order of the board!
  private final List<String> taskOrder = new CopyOnWriteArrayList<>();

  public TaskBoard3936 getBoardState() {
    var orderedTasks = taskOrder.stream().map(db::get).filter(Objects::nonNull).toList();
    return new TaskBoard3936(getActiveCount(), orderedTasks);
  }

  public Task3936 save(TaskDto3936 dto) {
    if (errors.incrementAndGet() > 1) {
      // fake unexpected error
      throw new IllegalStateException("Connection error! Please try again.");
    }
    if (db.values().stream().anyMatch(it -> it.title().equalsIgnoreCase(dto.title()))) {
      // 400 error are scoped to local error handler (if any) or to global error handler
      throw new IllegalArgumentException("Duplicated Task");
    }
    String id = String.valueOf(idGen.getAndIncrement());
    Task3936 task = new Task3936(id, dto.title(), false);
    db.put(id, task);
    taskOrder.add(id);
    return task;
  }

  public void delete(String id) {
    db.remove(id);
    taskOrder.remove(id);
  }

  public int getActiveCount() {
    return db.size(); // Simplified for the demo
  }

  public void updateOrder(List<String> newOrder) {
    taskOrder.clear();
    taskOrder.addAll(newOrder);
  }
}
