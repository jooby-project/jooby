/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.htmx;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;

import io.jooby.Context;
import io.jooby.ErrorHandler;
import io.jooby.Router;
import io.jooby.StatusCode;
import io.jooby.value.Value;

class HtmxErrorHandlerTest {

  private Context ctx;
  private Router router;
  private Logger logger;
  private LoggingEventBuilder logBuilder;
  private Value hxRequestValue;

  private boolean applyWasCalled;
  private HtmxResponse mockResponse;
  private HtmxErrorHandler htmxErrorHandler;

  @BeforeEach
  void setUp() {
    ctx = mock(Context.class);
    router = mock(Router.class);
    logger = mock(Logger.class);
    logBuilder = mock(LoggingEventBuilder.class);
    hxRequestValue = mock(Value.class);
    mockResponse = mock(HtmxResponse.class);

    when(ctx.getRouter()).thenReturn(router);
    when(router.getLog()).thenReturn(logger);
    when(logger.atLevel(any())).thenReturn(logBuilder);
    when(ctx.header("HX-Request")).thenReturn(hxRequestValue);

    applyWasCalled = false;

    // Create a concrete instance to test the default method behavior
    htmxErrorHandler =
        (context, cause, code) -> {
          applyWasCalled = true;
          return mockResponse;
        };
  }

  @Test
  void shouldIgnoreNonHtmxRequests() throws Exception {
    when(hxRequestValue.booleanValue(false)).thenReturn(false);

    ErrorHandler joobyHandler = htmxErrorHandler.toErrorHandler();
    joobyHandler.apply(ctx, new RuntimeException("Test"), StatusCode.SERVER_ERROR);

    // Ensure apply() was never reached
    assertTrue(!applyWasCalled, "Apply should not be called for non-HTMX requests");
    verify(mockResponse, never()).send(any());
  }

  @Test
  void shouldIgnoreHtmxDirectAccessExceptions() throws Exception {
    when(hxRequestValue.booleanValue(false)).thenReturn(true);

    ErrorHandler joobyHandler = htmxErrorHandler.toErrorHandler();
    HtmxDirectAccessException directAccessException =
        new HtmxDirectAccessException("Direct access block");

    joobyHandler.apply(ctx, directAccessException, StatusCode.NOT_ACCEPTABLE);

    // Ensure apply() was never reached
    assertTrue(!applyWasCalled, "Apply should not be called for HtmxDirectAccessException");
    verify(mockResponse, never()).send(any());
  }

  @Test
  void shouldHandleClientErrorsWithDebugLog() throws Exception {
    when(hxRequestValue.booleanValue(false)).thenReturn(true);

    ErrorHandler joobyHandler = htmxErrorHandler.toErrorHandler();
    RuntimeException cause = new RuntimeException("Validation Failed");

    // Act: 422 Unprocessable Entity (< 500)
    joobyHandler.apply(ctx, cause, StatusCode.UNPROCESSABLE_ENTITY);

    // Assert: Handled successfully
    assertTrue(applyWasCalled, "Apply should be called for valid HTMX client errors");
    verify(mockResponse).send(ctx);

    // Assert: Logged at DEBUG level
    verify(logger).atLevel(Level.DEBUG);
    verify(logBuilder).log(anyString(), any(Throwable.class));
  }

  @Test
  void shouldHandleServerErrorsWithErrorLog() throws Exception {
    when(hxRequestValue.booleanValue(false)).thenReturn(true);

    ErrorHandler joobyHandler = htmxErrorHandler.toErrorHandler();
    RuntimeException cause = new RuntimeException("Database Offline");

    // Act: 500 Server Error (>= 500)
    joobyHandler.apply(ctx, cause, StatusCode.SERVER_ERROR);

    // Assert: Handled successfully
    assertTrue(applyWasCalled, "Apply should be called for valid HTMX server errors");
    verify(mockResponse).send(ctx);

    // Assert: Logged at ERROR level
    verify(logger).atLevel(Level.ERROR);
    verify(logBuilder).log(anyString(), any(Throwable.class));
  }
}
