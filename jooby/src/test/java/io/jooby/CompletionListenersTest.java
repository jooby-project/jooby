/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

public class CompletionListenersTest {

  private Context ctx;
  private Router router;
  private Logger logger;

  @BeforeEach
  void setUp() {
    ctx = mock(Context.class);
    router = mock(Router.class);
    logger = mock(Logger.class);

    when(ctx.getRouter()).thenReturn(router);
    when(router.getLog()).thenReturn(logger);
    when(ctx.getMethod()).thenReturn("GET");
    when(ctx.getRequestPath()).thenReturn("/test");
  }

  @Test
  void shouldDoNothingWhenNoListeners() {
    CompletionListeners listeners = new CompletionListeners();
    listeners.run(ctx);
    verifyNoInteractions(ctx);
  }

  @Test
  void shouldRunListenersInReverseOrder() throws Exception {
    CompletionListeners listeners = new CompletionListeners();
    List<Integer> order = new ArrayList<>();

    listeners.addListener(c -> order.add(1));
    listeners.addListener(c -> order.add(2));
    listeners.addListener(c -> order.add(3));

    listeners.run(ctx);

    // Verify LIFO order
    java.util.List<Integer> expected = java.util.Arrays.asList(3, 2, 1);
    org.junit.jupiter.api.Assertions.assertEquals(expected, order);
  }

  @Test
  void shouldLogAndSuppressMultipleExceptions() throws Exception {
    CompletionListeners listeners = new CompletionListeners();

    RuntimeException ex1 = new RuntimeException("Error 1");
    RuntimeException ex2 = new RuntimeException("Error 2");

    listeners.addListener(
        c -> {
          throw ex1;
        });
    listeners.addListener(
        c -> {
          throw ex2;
        });

    listeners.run(ctx);

    // Verify Error 2 was the primary (since it's last in, first out)
    // and Error 1 was suppressed
    verify(logger).error(anyString(), eq("GET"), eq("/test"), eq(ex2));
    org.junit.jupiter.api.Assertions.assertEquals(1, ex2.getSuppressed().length);
    org.junit.jupiter.api.Assertions.assertEquals(ex1, ex2.getSuppressed()[0]);
  }

  @Test
  void shouldPropagateFatalExceptions() throws Exception {
    CompletionListeners listeners = new CompletionListeners();

    // OutOfMemoryError is considered fatal
    OutOfMemoryError fatal = new OutOfMemoryError("Fatal");
    listeners.addListener(
        c -> {
          throw fatal;
        });

    assertThrows(OutOfMemoryError.class, () -> listeners.run(ctx));

    // Ensure it was still logged before being rethrown
    verify(logger).error(anyString(), any(), any(), eq(fatal));
  }

  @Test
  void shouldPropagateSuppressedFatalException() throws Exception {
    CompletionListeners listeners = new CompletionListeners();

    OutOfMemoryError fatal = new OutOfMemoryError("Fatal");
    RuntimeException normal = new RuntimeException("Normal");

    // LIFO: normal runs first, then fatal
    listeners.addListener(
        c -> {
          throw fatal;
        });
    listeners.addListener(
        c -> {
          throw normal;
        });

    assertThrows(OutOfMemoryError.class, () -> listeners.run(ctx));
  }
}
