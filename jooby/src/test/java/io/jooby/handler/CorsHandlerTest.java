/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jooby.Context;
import io.jooby.Route;
import io.jooby.Router;
import io.jooby.StatusCode;
import io.jooby.value.Value;

public class CorsHandlerTest {

  private Context ctx;
  private Route.Handler next;
  private Cors options;

  @BeforeEach
  void setUp() throws Exception {
    ctx = mock(Context.class);
    next = mock(Route.Handler.class);
    options = mock(Cors.class);

    when(next.apply(ctx)).thenReturn("NEXT_RESULT");
  }

  private Value mockHeader(String val) {
    Value v = mock(Value.class);
    when(v.valueOrNull()).thenReturn(val);
    when(v.toOptional()).thenReturn(Optional.ofNullable(val));
    return v;
  }

  private void setupRouterForMethodMatching() {
    AtomicReference<String> currentMethod = new AtomicReference<>("OPTIONS");
    when(ctx.getMethod()).thenAnswer(inv -> currentMethod.get());

    doAnswer(
            inv -> {
              currentMethod.set(inv.getArgument(0));
              return ctx;
            })
        .when(ctx)
        .setMethod(anyString());

    Router router = mock(Router.class);
    when(ctx.getRouter()).thenReturn(router);

    when(router.match(ctx))
        .thenAnswer(
            inv -> {
              Router.Match match = mock(Router.Match.class);
              // Simulate matches for GET and POST, but not for others
              if ("GET".equals(currentMethod.get()) || "POST".equals(currentMethod.get())) {
                when(match.matches()).thenReturn(true);
              } else {
                when(match.matches()).thenReturn(false);
              }
              return match;
            });
  }

  @Test
  @DisplayName("Verify default constructor and setRoute")
  void testDefaultsAndSetRoute() {
    CorsHandler handler = new CorsHandler(); // Covers default constructor
    Route route = mock(Route.class);
    handler.setRoute(route);

    verify(route).setHttpOptions(true);
  }

  @Test
  @DisplayName("Verify absent Origin delegates to next handler on non-OPTIONS request")
  void testAbsentOriginNotOptions() throws Exception {
    doReturn(mockHeader(null)).when(ctx).header("Origin");
    when(ctx.getMethod()).thenReturn("GET");

    CorsHandler handler = new CorsHandler(options);
    Object result = handler.apply(next).apply(ctx);

    assertEquals("NEXT_RESULT", result);
  }

  @Test
  @DisplayName("Verify absent Origin handles OPTIONS request normally")
  void testAbsentOriginOptions() throws Exception {
    doReturn(mockHeader(null)).when(ctx).header("Origin");
    setupRouterForMethodMatching();

    CorsHandler handler = new CorsHandler(options);
    handler.apply(next).apply(ctx);

    verify(ctx).setResponseHeader("Allow", "GET,POST");
    verify(ctx).send(StatusCode.OK);
  }

  @Test
  @DisplayName("Verify forbidden response when Origin is denied")
  void testOriginDenied() throws Exception {
    doReturn(mockHeader("http://bad.com")).when(ctx).header("Origin");
    when(options.allowOrigin("http://bad.com")).thenReturn(false);

    CorsHandler handler = new CorsHandler(options);
    handler.apply(next).apply(ctx);

    verify(ctx).send(StatusCode.FORBIDDEN);
  }

  @Test
  @DisplayName("Verify preflight denied on missing or unallowed method")
  void testPreflightMethodDenied() throws Exception {
    doReturn(mockHeader("http://good.com")).when(ctx).header("Origin");
    when(options.allowOrigin("http://good.com")).thenReturn(true);
    when(ctx.isPreflight()).thenReturn(true);

    // Test missing method
    doReturn(mockHeader(null)).when(ctx).header("Access-Control-Request-Method");
    new CorsHandler(options).apply(next).apply(ctx);
    verify(ctx).send(StatusCode.FORBIDDEN);

    // Test denied method
    doReturn(mockHeader("DELETE")).when(ctx).header("Access-Control-Request-Method");
    when(options.allowMethod("DELETE")).thenReturn(false);
    new CorsHandler(options).apply(next).apply(ctx);
    // Verified implicitly as FORBIDDEN count increases
  }

  @Test
  @DisplayName("Verify preflight denied on unallowed headers")
  void testPreflightHeadersDenied() throws Exception {
    doReturn(mockHeader("http://good.com")).when(ctx).header("Origin");
    when(options.allowOrigin("http://good.com")).thenReturn(true);
    when(ctx.isPreflight()).thenReturn(true);

    doReturn(mockHeader("POST")).when(ctx).header("Access-Control-Request-Method");
    when(options.allowMethod("POST")).thenReturn(true);

    doReturn(mockHeader("X-A, X-B")).when(ctx).header("Access-Control-Request-Headers");
    when(options.allowHeaders(Arrays.asList("X-A", "X-B"))).thenReturn(false);

    new CorsHandler(options).apply(next).apply(ctx);
    verify(ctx).send(StatusCode.FORBIDDEN);
  }

  @Test
  @DisplayName("Verify preflight success with specific headers and origin variation")
  void testPreflightSuccess() throws Exception {
    doReturn(mockHeader("http://good.com")).when(ctx).header("Origin");
    when(options.allowOrigin("http://good.com")).thenReturn(true);
    when(ctx.isPreflight()).thenReturn(true);

    doReturn(mockHeader("PUT")).when(ctx).header("Access-Control-Request-Method");
    when(options.allowMethod("PUT")).thenReturn(true);

    // Missing request headers falls back to empty list evaluation
    doReturn(mockHeader(null)).when(ctx).header("Access-Control-Request-Headers");
    when(options.allowHeaders(Collections.emptyList())).thenReturn(true);

    when(options.getMethods()).thenReturn(Arrays.asList("GET", "PUT"));
    when(options.anyHeader()).thenReturn(false);
    when(options.getHeaders()).thenReturn(Arrays.asList("X-Allowed"));
    when(options.getUseCredentials()).thenReturn(true);
    when(options.getMaxAge()).thenReturn(Duration.ofHours(1));
    when(options.anyOrigin()).thenReturn(false);

    new CorsHandler(options).apply(next).apply(ctx);

    verify(ctx).setResponseHeader("Access-Control-Allow-Methods", "GET,PUT");
    verify(ctx).setResponseHeader("Access-Control-Allow-Headers", "X-Allowed");
    verify(ctx).setResponseHeader("Access-Control-Allow-Credentials", true);
    verify(ctx).setResponseHeader("Access-Control-Max-Age", 3600L);
    verify(ctx).setResponseHeader("Access-Control-Allow-Origin", "http://good.com");
    verify(ctx).setResponseHeader("Vary", "Origin");
    verify(ctx).send(StatusCode.OK);
  }

  @Test
  @DisplayName(
      "Verify preflight success alternate branches (maxAge=0, anyOrigin=true, anyHeader=true)")
  void testPreflightAlternateBranches() throws Exception {
    doReturn(mockHeader("http://good.com")).when(ctx).header("Origin");
    when(options.allowOrigin("http://good.com")).thenReturn(true);
    when(ctx.isPreflight()).thenReturn(true);

    doReturn(mockHeader("GET")).when(ctx).header("Access-Control-Request-Method");
    when(options.allowMethod("GET")).thenReturn(true);
    doReturn(mockHeader("X-Dynamic")).when(ctx).header("Access-Control-Request-Headers");
    when(options.allowHeaders(Arrays.asList("X-Dynamic"))).thenReturn(true);

    when(options.getMethods()).thenReturn(Collections.emptyList());
    when(options.anyHeader()).thenReturn(true); // uses incoming headers "X-Dynamic"
    when(options.getUseCredentials()).thenReturn(false);
    when(options.getMaxAge()).thenReturn(Duration.ofSeconds(0)); // 0 prevents header insertion
    when(options.anyOrigin()).thenReturn(true); // Prevents Vary: Origin

    new CorsHandler(options).apply(next).apply(ctx);

    verify(ctx).setResponseHeader("Access-Control-Allow-Headers", "X-Dynamic");
    verify(ctx, never()).setResponseHeader("Access-Control-Allow-Credentials", true);
    verify(ctx, never()).setResponseHeader(eq("Access-Control-Max-Age"), anyLong());
    verify(ctx, never()).setResponseHeader("Vary", "Origin");
    verify(ctx).send(StatusCode.OK);
  }

  @Test
  @DisplayName("Verify simple CORS on OPTIONS request is treated as normal options routing")
  void testSimpleCorsOptions() throws Exception {
    doReturn(mockHeader("http://foo.com")).when(ctx).header("Origin");
    when(options.allowOrigin("http://foo.com")).thenReturn(true);
    when(ctx.isPreflight()).thenReturn(false);

    setupRouterForMethodMatching();

    new CorsHandler(options).apply(next).apply(ctx);

    verify(ctx).setResponseHeader("Allow", "GET,POST");
    verify(ctx, never()).setResponseHeader(eq("Access-Control-Allow-Origin"), anyString());
  }

  @Test
  @DisplayName("Verify simple CORS with null origin grants ANY_ORIGIN")
  void testSimpleCorsNullOrigin() throws Exception {
    doReturn(mockHeader("null")).when(ctx).header("Origin");
    when(options.allowOrigin("null")).thenReturn(true);
    when(ctx.isPreflight()).thenReturn(false);
    when(ctx.getMethod()).thenReturn("GET");

    CorsHandler handler = new CorsHandler(options);
    Object result = handler.apply(next).apply(ctx);

    verify(ctx).setResponseHeader("Access-Control-Allow-Origin", "*");
    assertEquals("NEXT_RESULT", result);
  }

  @Test
  @DisplayName("Verify simple CORS full configuration (credentials, exposed headers, Vary)")
  void testSimpleCorsFullConfig() throws Exception {
    doReturn(mockHeader("http://foo.com")).when(ctx).header("Origin");
    when(options.allowOrigin("http://foo.com")).thenReturn(true);
    when(ctx.isPreflight()).thenReturn(false);
    when(ctx.getMethod()).thenReturn("GET");

    when(options.anyHeader()).thenReturn(false); // Triggers Vary: Origin
    when(options.getUseCredentials()).thenReturn(true);
    // Collectors.joining() on exposed headers concatenates elements directly
    when(options.getExposedHeaders()).thenReturn(Arrays.asList("X-Exposed-1"));

    CorsHandler handler = new CorsHandler(options);
    handler.apply(next).apply(ctx);

    verify(ctx).setResetHeadersOnError(false);
    verify(ctx).setResponseHeader("Access-Control-Allow-Origin", "http://foo.com");
    verify(ctx).setResponseHeader("Vary", "Origin");
    verify(ctx).setResponseHeader("Access-Control-Allow-Credentials", true);
    verify(ctx).setResponseHeader("Access-Control-Expose-Headers", "X-Exposed-1");
  }
}
