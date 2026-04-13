/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.opentelemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;

import io.jooby.Context;
import io.jooby.Route;
import io.jooby.Router;
import io.jooby.StatusCode;
import io.jooby.value.Value;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;

public class OtelHttpTracingTest {

  @RegisterExtension
  static final OpenTelemetryExtension otelTesting = OpenTelemetryExtension.create();

  private Context ctx;
  private Route route;
  private Route.Handler next;
  private Router router;

  @BeforeEach
  void setUp() {
    ctx = mock(Context.class);
    route = mock(Route.class);
    next = mock(Route.Handler.class);
    router = mock(Router.class);

    // Core HTTP routing mocks
    when(ctx.getMethod()).thenReturn("GET");
    when(ctx.getRequestPath()).thenReturn("/api/users/123");
    when(ctx.getRoute()).thenReturn(route);
    when(route.getPattern()).thenReturn("/api/users/{id}");
    when(ctx.getRouter()).thenReturn(router);

    // OpenTelemetry DI mocks (injecting the in-memory SDK)
    Tracer tracer = otelTesting.getOpenTelemetry().getTracer("test-tracer");
    when(ctx.require(Tracer.class)).thenReturn(tracer);
    when(ctx.require(OpenTelemetry.class)).thenReturn(otelTesting.getOpenTelemetry());

    // Header extraction mocks
    Value missingHeader = mock(Value.class);
    when(missingHeader.valueOrNull()).thenReturn(null);
    when(ctx.header(anyString())).thenReturn(missingHeader);
  }

  @Test
  void shouldTraceSuccessfulRequest() throws Throwable {
    // Arrange
    when(next.apply(ctx)).thenReturn("Success");
    when(ctx.getResponseCode()).thenReturn(StatusCode.OK);

    OtelHttpTracing filter = new OtelHttpTracing();
    Route.Handler wrapped = filter.apply(next);

    // Act
    Object result = wrapped.apply(ctx);

    // Trigger Jooby's onComplete callback
    ArgumentCaptor<Route.Complete> onCompleteCaptor = ArgumentCaptor.forClass(Route.Complete.class);
    verify(ctx).onComplete(onCompleteCaptor.capture());
    onCompleteCaptor.getValue().apply(ctx);

    // Assert
    assertEquals("Success", result);
    verify(ctx).setAttribute(any(String.class), any()); // Verifies span was put in context

    java.util.List<SpanData> spans = otelTesting.getSpans();
    assertEquals(1, spans.size());

    SpanData span = spans.get(0);
    assertEquals("GET /api/users/{id}", span.getName());
    assertEquals(SpanKind.SERVER, span.getKind());
    assertEquals(StatusData.unset(), span.getStatus());

    assertThat(span.getAttributes().asMap())
        .containsEntry(
            io.opentelemetry.api.common.AttributeKey.stringKey("http.request.method"), "GET")
        .containsEntry(
            io.opentelemetry.api.common.AttributeKey.stringKey("url.path"), "/api/users/123")
        .containsEntry(
            io.opentelemetry.api.common.AttributeKey.stringKey("http.route"), "/api/users/{id}")
        .containsEntry(
            io.opentelemetry.api.common.AttributeKey.longKey("http.response.status_code"), 200L);
  }

  @Test
  void shouldRecordExceptionAndFailSpan() throws Throwable {
    // Arrange
    RuntimeException exception = new RuntimeException("Database timeout");
    when(next.apply(ctx)).thenThrow(exception);
    when(router.errorCode(exception)).thenReturn(StatusCode.SERVER_ERROR);

    OtelHttpTracing filter = new OtelHttpTracing();
    Route.Handler wrapped = filter.apply(next);

    // Act & Assert Exception
    assertThrows(RuntimeException.class, () -> wrapped.apply(ctx));

    // Notice we do NOT trigger onComplete here because Jooby handles exception propagation,
    // but the catch block in the filter records the exception immediately.
    // Span.end() relies on the container eventually triggering onComplete. For the sake of the
    // test,
    // we manually trigger it to finalize the span state as Jooby would.
    when(ctx.getResponseCode()).thenReturn(StatusCode.SERVER_ERROR);
    ArgumentCaptor<Route.Complete> onCompleteCaptor = ArgumentCaptor.forClass(Route.Complete.class);
    verify(ctx).onComplete(onCompleteCaptor.capture());
    onCompleteCaptor.getValue().apply(ctx);

    // Assert Span
    java.util.List<SpanData> spans = otelTesting.getSpans();
    assertEquals(1, spans.size());

    SpanData span = spans.get(0);
    assertEquals(StatusData.error(), span.getStatus());
    assertEquals(1, span.getEvents().size());
    assertEquals("exception", span.getEvents().get(0).getName());

    assertThat(span.getAttributes().asMap())
        .containsEntry(
            io.opentelemetry.api.common.AttributeKey.longKey("http.response.status_code"), 500L);
  }

  @Test
  void shouldMarkSpanAsErrorOn500StatusCode() throws Throwable {
    // Arrange (Code executes fine, but sets a 500 status internally)
    when(next.apply(ctx)).thenReturn("Internal Failure");
    when(ctx.getResponseCode()).thenReturn(StatusCode.SERVER_ERROR);

    OtelHttpTracing filter = new OtelHttpTracing();
    Route.Handler wrapped = filter.apply(next);

    // Act
    wrapped.apply(ctx);

    ArgumentCaptor<Route.Complete> onCompleteCaptor = ArgumentCaptor.forClass(Route.Complete.class);
    verify(ctx).onComplete(onCompleteCaptor.capture());
    onCompleteCaptor.getValue().apply(ctx);

    // Assert
    java.util.List<SpanData> spans = otelTesting.getSpans();
    assertEquals(1, spans.size());

    SpanData span = spans.get(0);
    assertEquals(StatusData.error(), span.getStatus());
    assertThat(span.getAttributes().asMap())
        .containsEntry(
            io.opentelemetry.api.common.AttributeKey.longKey("http.response.status_code"), 500L);
  }

  @Test
  void joobyRequestGetterExtractsHeaders() {
    // Arrange
    when(ctx.headerMap())
        .thenReturn(
            Map.of("traceparent", "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01"));

    Value mockHeaderValue = mock(Value.class);
    when(mockHeaderValue.valueOrNull())
        .thenReturn("00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01");
    when(ctx.header("traceparent")).thenReturn(mockHeaderValue);

    // Act
    Iterable<String> keys = OtelHttpTracing.JoobyRequestGetter.INSTANCE.keys(ctx);
    String headerVal = OtelHttpTracing.JoobyRequestGetter.INSTANCE.get(ctx, "traceparent");

    // Assert
    assertThat(keys).containsExactly("traceparent");
    assertEquals("00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01", headerVal);
  }
}
