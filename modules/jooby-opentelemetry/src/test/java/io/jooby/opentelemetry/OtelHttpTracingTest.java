/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.opentelemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
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

    // OtelContextExtractor mock
    OtelContextExtractor extractor = mock(OtelContextExtractor.class);
    when(ctx.require(OtelContextExtractor.class)).thenReturn(extractor);
    when(extractor.extract(ctx)).thenReturn(io.opentelemetry.context.Context.current());

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

    // Verify both attributes were saved to the Jooby context
    verify(ctx).setAttribute(eq("otel-span"), any(Span.class));

    ArgumentCaptor<io.opentelemetry.context.Context> otelCtxCaptor =
        ArgumentCaptor.forClass(io.opentelemetry.context.Context.class);
    verify(ctx)
        .setAttribute(
            eq(io.opentelemetry.context.Context.class.getName()), otelCtxCaptor.capture());

    // Ensure the captured context actually contains the span we just created
    io.opentelemetry.context.Context capturedContext = otelCtxCaptor.getValue();
    assertNotNull(Span.fromContext(capturedContext));

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
  void shouldExtractContextAndCreateSpan() throws Throwable {
    // 1. Arrange - Core Mocks
    var ctx = mock(Context.class);
    var route = mock(Route.class);
    var next = mock(Route.Handler.class);

    // 2. Arrange - OTel Mocks
    var tracer = mock(Tracer.class);
    var spanBuilder = mock(SpanBuilder.class);
    var span = mock(Span.class);
    var scope = mock(Scope.class);

    // 3. Arrange - The new Extractor Mocks
    var extractor = mock(OtelContextExtractor.class);
    var parentOtelContext = mock(io.opentelemetry.context.Context.class);

    // Mock Jooby Routing State
    when(ctx.getMethod()).thenReturn("GET");
    when(ctx.getRequestPath()).thenReturn("/api/users/123");
    when(route.getPattern()).thenReturn("/api/users/{id}");
    when(ctx.getRoute()).thenReturn(route);

    // Wire up the registry requires
    when(ctx.require(Tracer.class)).thenReturn(tracer);
    when(ctx.require(OtelContextExtractor.class)).thenReturn(extractor);

    // Mock the Extractor behavior
    when(extractor.extract(ctx)).thenReturn(parentOtelContext);

    // Mock the OpenTelemetry Builder Chain
    when(tracer.spanBuilder("GET /api/users/{id}")).thenReturn(spanBuilder);
    when(spanBuilder.setParent(parentOtelContext)).thenReturn(spanBuilder);
    when(spanBuilder.setSpanKind(SpanKind.SERVER)).thenReturn(spanBuilder);
    when(spanBuilder.setAttribute(anyString(), anyString())).thenReturn(spanBuilder);
    when(spanBuilder.startSpan()).thenReturn(span);
    when(span.makeCurrent()).thenReturn(scope);

    // Act
    var filter = new OtelHttpTracing();
    filter.apply(next).apply(ctx);

    // Assert
    verify(extractor).extract(ctx);
    verify(spanBuilder).setParent(parentOtelContext);

    // Verify the span was stored in the jooby context
    verify(ctx).setAttribute("otel-span", span);

    // Safely verify the context was saved without accidentally evaluating Context.current() outside
    // the scope
    verify(ctx)
        .setAttribute(
            eq(io.opentelemetry.context.Context.class.getName()),
            any(io.opentelemetry.context.Context.class));

    verify(next).apply(ctx);
  }
}
