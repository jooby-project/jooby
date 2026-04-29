/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.jooby.Context;
import io.jooby.Route;
import io.jooby.Router;
import io.jooby.StatusCode;
import io.jooby.value.Value;

public class AccessLogHandlerTest {

  private Context ctx;
  private Route.Handler next;

  @BeforeEach
  void setUp() throws Exception {
    ctx = mock(Context.class);
    next = mock(Route.Handler.class);
    when(next.apply(ctx)).thenReturn("OK");

    // Default mock behavior for standard request
    when(ctx.getRemoteAddress()).thenReturn("192.168.1.1");
    when(ctx.getMethod()).thenReturn(Router.GET);
    when(ctx.getRequestPath()).thenReturn("/api/users");
    when(ctx.queryString()).thenReturn("?status=active");
    when(ctx.getProtocol()).thenReturn("HTTP/1.1");
    when(ctx.getResponseCode()).thenReturn(StatusCode.OK);
    when(ctx.getResponseLength()).thenReturn(512L);
  }

  @Test
  @DisplayName("Verify default NCSA log formatting without an authenticated user")
  void testDefaultLogFormat() throws Exception {
    when(ctx.getUser()).thenReturn(null);

    AtomicReference<String> logResult = new AtomicReference<>();
    AccessLogHandler handler = new AccessLogHandler().log(logResult::set);

    triggerAndCaptureLog(handler);

    String result = logResult.get();
    assertNotNull(result);
    // 192.168.1.1 - - [Date] "GET /api/users?status=active HTTP/1.1" 200 512 {latency}
    assertTrue(result.startsWith("192.168.1.1 - - ["));
    assertTrue(result.contains("] \"GET /api/users?status=active HTTP/1.1\" 200 512 "));
  }

  @Test
  @DisplayName("Verify NCSA log formatting with an authenticated user")
  void testAuthenticatedUserLogFormat() throws Exception {
    when(ctx.getUser()).thenReturn("admin-user");

    AtomicReference<String> logResult = new AtomicReference<>();
    AccessLogHandler handler = new AccessLogHandler().log(logResult::set);

    triggerAndCaptureLog(handler);

    String result = logResult.get();
    assertNotNull(result);
    // 192.168.1.1 - admin-user [Date] ...
    assertTrue(result.startsWith("192.168.1.1 - admin-user ["));
  }

  @Test
  @DisplayName("Verify custom user ID provider")
  void testCustomUserIdProvider() throws Exception {
    AtomicReference<String> logResult = new AtomicReference<>();
    AccessLogHandler handler = new AccessLogHandler(c -> "custom-id").log(logResult::set);

    triggerAndCaptureLog(handler);

    String result = logResult.get();
    assertTrue(result.startsWith("192.168.1.1 - custom-id ["));
  }

  @Test
  @DisplayName("Verify missing response length and extended headers logic")
  void testMissingResponseLengthAndHeaders() throws Exception {
    when(ctx.getUser()).thenReturn(null);
    when(ctx.getResponseLength()).thenReturn(-1L); // Triggers DASH for length

    // Mock headers
    Value userAgent = mock(Value.class);
    when(userAgent.valueOrNull()).thenReturn("Mozilla/5.0");
    when(ctx.header("User-Agent")).thenReturn(userAgent);

    Value referer = mock(Value.class);
    when(referer.valueOrNull()).thenReturn(null); // Triggers DASH for missing header
    when(ctx.header("Referer")).thenReturn(referer);

    when(ctx.getResponseHeader("X-Request-Id")).thenReturn("req-123");

    AtomicReference<String> logResult = new AtomicReference<>();
    AccessLogHandler handler =
        new AccessLogHandler()
            .extended() // Adds User-Agent and Referer
            .responseHeader("X-Request-Id")
            .log(logResult::set);

    triggerAndCaptureLog(handler);

    String result = logResult.get();
    // Validate length is DASH (-)
    assertTrue(result.contains("\" 200 - "));

    // Validate appended headers: "Mozilla/5.0" "-" "req-123"
    assertTrue(result.endsWith(" \"Mozilla/5.0\" \"-\" \"req-123\""));
  }

  @Test
  @DisplayName("Verify custom request headers configuration")
  void testRequestHeaderConfiguration() throws Exception {
    Value customHeader = mock(Value.class);
    when(customHeader.valueOrNull()).thenReturn("custom-value");
    when(ctx.header("X-Custom")).thenReturn(customHeader);

    AtomicReference<String> logResult = new AtomicReference<>();
    AccessLogHandler handler = new AccessLogHandler().requestHeader("X-Custom").log(logResult::set);

    triggerAndCaptureLog(handler);

    String result = logResult.get();
    assertTrue(result.endsWith(" \"custom-value\""));
  }

  @Test
  @DisplayName("Verify various dateFormatter overrides")
  void testDateFormatterOverrides() throws Exception {
    // 1. Test ZoneId
    AccessLogHandler h1 = new AccessLogHandler().dateFormatter(ZoneId.of("UTC"));
    assertNotNull(h1);

    // 2. Test DateTimeFormatter
    AccessLogHandler h2 = new AccessLogHandler().dateFormatter(DateTimeFormatter.ISO_INSTANT);
    assertNotNull(h2);

    // 3. Test Function<Long, String>
    AtomicReference<String> logResult = new AtomicReference<>();
    AccessLogHandler h3 =
        new AccessLogHandler().dateFormatter(ts -> "STATIC_DATE").log(logResult::set);

    triggerAndCaptureLog(h3);
    String result = logResult.get();
    assertTrue(result.contains(" [STATIC_DATE] "));
  }

  /** Helper to execute the pipeline and manually trigger the context.onComplete callback. */
  private void triggerAndCaptureLog(AccessLogHandler handler) throws Exception {
    Route.Handler pipeline = handler.apply(next);

    // Execute route
    Object response = pipeline.apply(ctx);
    assertEquals("OK", response);

    // Capture and execute the onComplete callback
    ArgumentCaptor<Route.Complete> captor = ArgumentCaptor.forClass(Route.Complete.class);
    verify(ctx).onComplete(captor.capture());

    Route.Complete completeCallback = captor.getValue();
    completeCallback.apply(ctx);
  }
}
