/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jooby.Context;
import io.jooby.Route;
import io.jooby.internal.ContextInitializer;

class PostDispatchInitializerHandlerTest {

  private ContextInitializer initializer;
  private Route.Handler next;
  private Context ctx;

  @BeforeEach
  void setUp() {
    initializer = mock(ContextInitializer.class);
    next = mock(Route.Handler.class);
    ctx = mock(Context.class);
  }

  @Test
  @DisplayName("Verify successful initialization and delegation to the next handler")
  void testSuccessfulExecution() throws Exception {
    Object expectedResponse = "Success";
    when(next.apply(ctx)).thenReturn(expectedResponse);

    PostDispatchInitializerHandler filter = new PostDispatchInitializerHandler(initializer);
    Route.Handler decoratedHandler = filter.apply(next);

    Object result = decoratedHandler.apply(ctx);

    // Verify the initializer ran before the next handler
    verify(initializer).apply(ctx);
    verify(next).apply(ctx);

    // Verify the result from the next handler is returned unmodified
    assertEquals(expectedResponse, result);
    verify(ctx, never()).sendError(any());
  }

  @Test
  @DisplayName("Verify exception thrown by the initializer is caught and routed to sendError")
  void testExceptionInInitializer() throws Exception {
    RuntimeException initError = new RuntimeException("Initialization failed");
    doThrow(initError).when(initializer).apply(ctx);

    PostDispatchInitializerHandler filter = new PostDispatchInitializerHandler(initializer);
    Route.Handler decoratedHandler = filter.apply(next);

    Object result = decoratedHandler.apply(ctx);

    verify(initializer).apply(ctx);

    // Ensure the next handler is NEVER called if initialization fails
    verify(next, never()).apply(ctx);

    // Verify the error was sent to the context and returned
    verify(ctx).sendError(initError);
    assertEquals(initError, result);
  }

  @Test
  @DisplayName(
      "Verify exception thrown by the downstream handler is caught and routed to sendError")
  void testExceptionInNextHandler() throws Exception {
    RuntimeException handlerError = new RuntimeException("Handler failed");
    when(next.apply(ctx)).thenThrow(handlerError);

    PostDispatchInitializerHandler filter = new PostDispatchInitializerHandler(initializer);
    Route.Handler decoratedHandler = filter.apply(next);

    Object result = decoratedHandler.apply(ctx);

    verify(initializer).apply(ctx);
    verify(next).apply(ctx);

    // Verify the error was sent to the context and returned
    verify(ctx).sendError(handlerError);
    assertEquals(handlerError, result);
  }
}
