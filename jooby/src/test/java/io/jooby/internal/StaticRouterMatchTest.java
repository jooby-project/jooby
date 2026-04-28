/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jooby.Context;
import io.jooby.Route;

public class StaticRouterMatchTest {

  private Route route;
  private StaticRouterMatch match;

  @BeforeEach
  void setUp() {
    route = mock(Route.class);
    match = new StaticRouterMatch(route);
  }

  @Test
  @DisplayName("Verify identity properties of a static match")
  void testProperties() {
    assertTrue(match.matches());
    assertSame(route, match.route());

    Map<String, String> pathMap = match.pathMap();
    assertTrue(pathMap.isEmpty());
  }

  @Test
  @DisplayName("Verify successful execution of route handler")
  void testExecuteSuccess() throws Exception {
    Context ctx = mock(Context.class);
    Route.Handler handler = mock(Route.Handler.class);
    Object expectedResult = "success";

    when(handler.apply(ctx)).thenReturn(expectedResult);

    Object result = match.execute(ctx, handler);

    // Verify route was set on context and handler was called
    verify(ctx).setRoute(route);
    assertEquals(expectedResult, result);
  }

  @Test
  @DisplayName("Verify error handling path during execution")
  void testExecuteFailure() throws Exception {
    Context ctx = mock(Context.class);
    Route.Handler handler = mock(Route.Handler.class);
    Exception exception = new RuntimeException("fail");

    when(handler.apply(ctx)).thenThrow(exception);

    Object result = match.execute(ctx, handler);

    // Verify route was set, error was sent to context, and exception was returned
    verify(ctx).setRoute(route);
    verify(ctx).sendError(exception);
    assertSame(exception, result);
  }

  @Test
  @DisplayName("Verify execute delegates to the route's own pipeline")
  void testExecuteDefaultPipeline() throws Exception {
    Context ctx = mock(Context.class);
    Route.Handler pipeline = mock(Route.Handler.class);

    when(route.getPipeline()).thenReturn(pipeline);
    when(pipeline.apply(ctx)).thenReturn("piped");

    Object result = match.execute(ctx);

    assertEquals("piped", result);
    verify(ctx).setRoute(route);
  }
}
