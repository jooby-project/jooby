/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

public class ForwardingExecutorTest {

  @Test
  public void testExecuteWhenNotReady() {
    ForwardingExecutor forwarding = new ForwardingExecutor();
    // Default state: executor is null

    Runnable task = () -> {};
    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> forwarding.execute(task));
    assertEquals("Worker executor not ready", ex.getMessage());
  }

  @Test
  public void testExecuteWhenReady() {
    ForwardingExecutor forwarding = new ForwardingExecutor();
    Executor mockExecutor = mock(Executor.class);

    // Set the delegate
    forwarding.executor = mockExecutor;

    Runnable task = mock(Runnable.class);
    forwarding.execute(task);

    // Verify delegation
    verify(mockExecutor).execute(task);
  }

  @Test
  public void testActualTaskExecution() {
    ForwardingExecutor forwarding = new ForwardingExecutor();
    AtomicBoolean ran = new AtomicBoolean(false);

    // Simple direct executor implementation
    forwarding.executor = Runnable::run;

    forwarding.execute(() -> ran.set(true));

    assertTrue(ran.get(), "The task should have been executed by the delegate");
  }
}
