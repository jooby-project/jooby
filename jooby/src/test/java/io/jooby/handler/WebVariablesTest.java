/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jooby.Context;
import io.jooby.Route;

public class WebVariablesTest {

  private Context ctx;
  private Route.Handler next;

  @BeforeEach
  void setUp() throws Exception {
    ctx = mock(Context.class);
    next = mock(Route.Handler.class);
    when(next.apply(ctx)).thenReturn("Result");
  }

  @Test
  @DisplayName(
      "Verify default scope, root context path (converted to empty string), and missing user")
  void testDefaultScopeAndRootContext() throws Exception {
    when(ctx.getContextPath()).thenReturn("/");
    when(ctx.getRequestPath()).thenReturn("/home");
    when(ctx.getUser()).thenReturn(null);

    WebVariables filter = new WebVariables();
    Route.Handler handler = filter.apply(next);

    Object result = handler.apply(ctx);

    assertEquals("Result", result);
    verify(ctx).setAttribute("contextPath", "");
    verify(ctx).setAttribute("path", "/home");
    verify(ctx, never()).setAttribute(eq("user"), any());
  }

  @Test
  @DisplayName("Verify custom scope, explicit context path, and authenticated user")
  void testCustomScopeAndExplicitContext() throws Exception {
    when(ctx.getContextPath()).thenReturn("/myapp");
    when(ctx.getRequestPath()).thenReturn("/myapp/dashboard");
    Object userObj = "admin-user";
    when(ctx.getUser()).thenReturn(userObj);

    WebVariables filter = new WebVariables("app");
    Route.Handler handler = filter.apply(next);

    Object result = handler.apply(ctx);

    assertEquals("Result", result);
    verify(ctx).setAttribute("app.contextPath", "/myapp");
    verify(ctx).setAttribute("app.path", "/myapp/dashboard");
    verify(ctx).setAttribute("app.user", userObj);
  }
}
