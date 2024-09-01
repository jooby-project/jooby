/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.dbscheduler;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.github.kagkarlsson.scheduler.CurrentlyExecuting;
import com.github.kagkarlsson.scheduler.ScheduledExecution;
import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.SchedulerState;
import com.github.kagkarlsson.scheduler.task.Execution;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import io.jooby.Jooby;
import io.jooby.exception.NotFoundException;

public class DbSchedulerApp extends Jooby {
  {
    get(
        "/",
        ctx -> {
          var scheduler = require(Scheduler.class);
          return toList(scheduler.getScheduledExecutions());
        });
    get(
        "/running",
        ctx -> {
          var scheduler = require(Scheduler.class);
          return scheduler.getCurrentlyExecuting().stream().map(DbSchedulerApp::toMap).toList();
        });
    get(
        "/state",
        ctx -> {
          var scheduler = require(Scheduler.class);
          return schedulerState(scheduler);
        });
    get(
        "/pause",
        ctx -> {
          var scheduler = require(Scheduler.class);
          scheduler.pause();
          return schedulerState(scheduler);
        });
    get(
        "/resume",
        ctx -> {
          var scheduler = require(Scheduler.class);
          scheduler.resume();
          return schedulerState(scheduler);
        });
    get(
        "/{taskName}",
        ctx -> {
          var scheduler = require(Scheduler.class);
          var taskName = ctx.path("taskName").value();
          return toMap(taskByName(scheduler, taskName));
        });
    get(
        "/{taskName}/reschedule",
        ctx -> {
          var scheduler = require(Scheduler.class);
          var taskName = ctx.path("taskName").value();
          var running =
              scheduler.getCurrentlyExecuting().stream()
                  .filter(it -> it.getTaskInstance().getTaskName().equals(taskName))
                  .findFirst();
          if (running.isPresent()) {
            return toMap(running.get());
          }
          var task = taskByName(scheduler, taskName);
          scheduler.reschedule(
              new TaskInstance<>(
                  task.getTaskInstance().getTaskName(), task.getTaskInstance().getId()),
              Instant.now());
          return toMap(task);
        });
  }

  private Map<String, String> schedulerState(Scheduler scheduler) {
    return Map.of("state", toStringState(scheduler.getSchedulerState()));
  }

  private static ScheduledExecution<Object> taskByName(Scheduler scheduler, String taskName) {
    return scheduler.getScheduledExecutionsForTask(taskName).stream()
        .findFirst()
        .orElseThrow(() -> new NotFoundException(taskName));
  }

  private static List<Map<String, Object>> toList(List<ScheduledExecution<Object>> executions) {
    List<Map<String, Object>> result = new ArrayList<>();
    for (var execution : executions) {
      result.add(toMap(execution));
    }
    return result;
  }

  private static Map<String, Object> toMap(CurrentlyExecuting execution) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("taskInstance", execution.getTaskInstance());
    result.put("duration", execution.getDuration());
    result.put("heartbeatState", execution.getHeartbeatState());
    result.put("execution", toMap(execution.getExecution()));
    return result;
  }

  private static Object toMap(Execution execution) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("executionTime", formatInstant(execution.executionTime));
    result.put("consecutiveFailures", execution.consecutiveFailures);
    result.put("lastFailure", formatInstant(execution.lastFailure));
    result.put("lastHeartbeat", formatInstant(execution.lastHeartbeat));
    result.put("lastSuccess", formatInstant(execution.lastSuccess));
    result.put("picked", execution.picked);
    result.put("pickedBy", execution.pickedBy);
    result.put("version", execution.version);
    return result;
  }

  private static String formatInstant(Instant instant) {
    return instant == null ? null : DateTimeFormatter.ISO_INSTANT.format(instant);
  }

  private static Map<String, Object> toMap(ScheduledExecution<Object> execution) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("executionTime", formatInstant(execution.getExecutionTime()));
    result.put("taskInstance", execution.getTaskInstance());
    result.put("consecutiveFailures", execution.getConsecutiveFailures());
    result.put("lastFailure", formatInstant(execution.getLastFailure()));
    result.put("lastSuccess", formatInstant(execution.getLastSuccess()));
    result.put("picked", execution.isPicked());
    result.put("pickedBy", execution.getPickedBy());
    result.put("data", execution.getData());
    return result;
  }

  private String toStringState(SchedulerState schedulerState) {
    if (schedulerState.isShuttingDown()) {
      return "SHUTTING_DOWN";
    }
    if (schedulerState.isPaused()) {
      return "PAUSED";
    }
    if (schedulerState.isStarted()) {
      return "STARTED";
    }
    throw new IllegalStateException("Unknown scheduler state");
  }
}
