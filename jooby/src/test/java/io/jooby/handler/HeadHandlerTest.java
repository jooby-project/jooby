/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.jooby.Context;
import io.jooby.Route;
import io.jooby.Router;

public class HeadHandlerTest {

  private Context ctx;
  private Route.Handler next;

  @BeforeEach
  void setUp() {
    ctx = mock(Context.class);
    next = mock(Route.Handler.class);

    // Fix: DefaultHandler expects the context to have a Route with an Encoder.
    // RETURNS_DEEP_STUBS automatically mocks chained calls like getRoute().getEncoder().encode()
    Route route = mock(Route.class, RETURNS_DEEP_STUBS);
    when(ctx.getRoute()).thenReturn(route);
  }

  @Test
  @DisplayName("Verify setRoute enables HTTP HEAD support on the route")
  void testSetRoute() {
    HeadHandler handler = new HeadHandler();
    Route route = mock(Route.class);

    handler.setRoute(route);

    verify(route).setHttpHead(true);
  }

  @Test
  @DisplayName("Verify non-HEAD requests bypass the HeadContext wrapper")
  void testNonHeadRequest() throws Exception {
    when(ctx.getMethod()).thenReturn(Router.GET);
    when(next.apply(ctx)).thenReturn("GET_RESULT");

    HeadHandler handler = new HeadHandler();
    Object result = handler.apply(next).apply(ctx);

    // Verify the result is returned directly and the original context is used
    assertEquals("GET_RESULT", result);
    verify(next).apply(ctx);
  }

  @Test
  @DisplayName(
      "Verify HEAD requests are wrapped in a HeadContext and routed through DefaultHandler")
  void testHeadRequest() throws Exception {
    when(ctx.getMethod()).thenReturn(Router.HEAD);

    // The DefaultHandler will eventually call next.apply() with the wrapped context
    when(next.apply(any(Context.class))).thenReturn("HEAD_RESULT");

    HeadHandler handler = new HeadHandler();
    Object result = handler.apply(next).apply(ctx);

    assertEquals("HEAD_RESULT", result);

    // Capture the context passed to the next handler to ensure it was wrapped
    ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
    verify(next).apply(contextCaptor.capture());

    Context wrappedContext = contextCaptor.getValue();

    // We check the simple class name to verify the wrapper without needing
    // to explicitly import the internal io.jooby.internal.HeadContext class
    assertEquals("HeadContext", wrappedContext.getClass().getSimpleName());
  }
}
