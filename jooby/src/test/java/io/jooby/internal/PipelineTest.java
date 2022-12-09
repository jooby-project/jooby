/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Type;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.Test;

import io.jooby.ExecutionMode;
import io.jooby.MessageEncoder;
import io.jooby.Route;
import io.jooby.internal.handler.DispatchHandler;
import io.jooby.internal.handler.LinkedHandler;
import io.jooby.internal.handler.SendCharSequence;
import io.jooby.internal.handler.WorkerHandler;

public class PipelineTest {

  @Test
  public void eventLoopDoesNothingOnSimpleTypes() {
    Route.Handler h = ctx -> "OK";
    LinkedHandler pipeline =
        (LinkedHandler) pipeline(route(String.class, h), ExecutionMode.EVENT_LOOP);
    assertTrue(pipeline instanceof SendCharSequence, pipeline.toString());
    assertTrue(pipeline.next() == h, "found: " + pipeline.next() + ", expected: " + h.getClass());
  }

  @Test
  public void eventLoopAlwaysDispatchToExecutorOnSimpleTypes() {
    Executor executor = task -> {};
    Route.Handler h = ctx -> "OK";
    LinkedHandler pipeline =
        (LinkedHandler) pipeline(route(String.class, h), ExecutionMode.EVENT_LOOP, executor);
    assertTrue(pipeline instanceof DispatchHandler, "found: " + pipeline);
    Route.Handler next = pipeline.next();
    assertTrue(next instanceof SendCharSequence);
    next = ((LinkedHandler) next).next();
    assertTrue(next == h, "found: " + next + ", expected: " + h.getClass());
  }

  @Test
  public void workerDoesNothingOnSimpleTypes() {
    Route.Handler h = ctx -> "OK";
    LinkedHandler pipeline = (LinkedHandler) pipeline(route(String.class, h), ExecutionMode.WORKER);
    assertTrue(pipeline instanceof WorkerHandler, "found: " + pipeline);
    Route.Handler next = pipeline.next();
    assertTrue(next instanceof SendCharSequence);
    next = ((LinkedHandler) next).next();
    assertTrue(next == h, "found: " + next + ", expected: " + h.getClass());
  }

  @Test
  public void workerAlwaysDispatchToExecutorOnSimpleTypes() {
    Executor executor = task -> {};
    Route.Handler h = ctx -> "OK";
    LinkedHandler pipeline =
        (LinkedHandler) pipeline(route(String.class, h), ExecutionMode.WORKER, executor);
    assertTrue(pipeline instanceof DispatchHandler, "found: " + pipeline);
    Route.Handler next = pipeline.next();
    assertTrue(next instanceof SendCharSequence);
    next = ((LinkedHandler) next).next();
    assertTrue(next == h, "found: " + next + ", expected: " + h.getClass());
  }

  private Route.Handler pipeline(Route route, ExecutionMode mode) {
    return pipeline(route, mode, null);
  }

  private Route.Handler pipeline(Route route, ExecutionMode mode, Executor executor) {
    return Pipeline.build(route, mode, executor, null, null);
  }

  private Route route(Type returnType, Route.Handler handler) {
    return new Route("GET", "/", handler)
        .setReturnType(returnType)
        .setEncoder(MessageEncoder.TO_STRING);
  }
}
