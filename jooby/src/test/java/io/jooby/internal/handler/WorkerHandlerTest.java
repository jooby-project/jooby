/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.jooby.Context;
import io.jooby.Route;

class WorkerHandlerTest {

  private Context ctx;
  private Route.Handler next;

  @BeforeEach
  void setUp() {
    ctx = mock(Context.class);
    next = mock(Route.Handler.class);
  }

  @Test
  @DisplayName("Verify toString returns 'worker'")
  void testToString() {
    assertEquals("worker", WorkerHandler.WORKER.toString());
  }

  @Test
  @DisplayName("Verify successful execution inside the dispatched worker thread")
  void testSuccessfulExecution() throws Throwable {
    Route.Handler decoratedHandler = WorkerHandler.WORKER.apply(next);

    // Call the outer handler, which delegates to ctx.dispatch(Runnable)
    decoratedHandler.apply(ctx);

    // Capture the Runnable that was passed to dispatch()
    ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
    verify(ctx).dispatch(runnableCaptor.capture());

    // Execute the captured Runnable to simulate the worker thread picking it up
    Runnable workerTask = runnableCaptor.getValue();
    workerTask.run();

    // Verify the downstream handler was called and no errors were sent
    verify(next).apply(ctx);
    verify(ctx, never()).sendError(any());
  }

  @Test
  @DisplayName("Verify exceptions inside the dispatched worker thread are sent to ctx.sendError()")
  void testExceptionInWorkerExecution() throws Throwable {
    Route.Handler decoratedHandler = WorkerHandler.WORKER.apply(next);

    // Call the outer handler, which delegates to ctx.dispatch(Runnable)
    decoratedHandler.apply(ctx);

    // Capture the Runnable that was passed to dispatch()
    ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
    verify(ctx).dispatch(runnableCaptor.capture());

    // Simulate the downstream handler throwing an exception
    RuntimeException workerError = new RuntimeException("Worker execution failed");
    when(next.apply(ctx)).thenThrow(workerError);

    // Execute the captured Runnable
    Runnable workerTask = runnableCaptor.getValue();
    workerTask.run();

    // Verify the downstream handler was called and the error was safely routed to context
    verify(next).apply(ctx);
    verify(ctx).sendError(workerError);
  }
}
