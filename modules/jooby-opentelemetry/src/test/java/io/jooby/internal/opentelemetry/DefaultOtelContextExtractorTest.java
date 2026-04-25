/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.opentelemetry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jooby.Context;
import io.jooby.value.Value;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;

class DefaultOtelContextExtractorTest {

  private OpenTelemetry otel;
  private ContextPropagators propagators;
  private TextMapPropagator textMapPropagator;
  private Context joobyCtx;
  private io.opentelemetry.context.Context otelCtx;

  private DefaultOtelContextExtractor extractor;

  @BeforeEach
  void setUp() {
    otel = mock(OpenTelemetry.class);
    propagators = mock(ContextPropagators.class);
    textMapPropagator = mock(TextMapPropagator.class);
    joobyCtx = mock(Context.class);
    otelCtx = mock(io.opentelemetry.context.Context.class);

    when(otel.getPropagators()).thenReturn(propagators);
    when(propagators.getTextMapPropagator()).thenReturn(textMapPropagator);

    extractor = new DefaultOtelContextExtractor(otel);
  }

  @Test
  void shouldReturnCachedContextWithoutParsingHeaders() {
    // Arrange: Simulate OtelHttpTracing already running and saving the context
    when(joobyCtx.getAttribute(io.opentelemetry.context.Context.class.getName()))
        .thenReturn(otelCtx);

    // Act
    io.opentelemetry.context.Context result = extractor.extract(joobyCtx);

    // Assert
    assertSame(otelCtx, result, "Should return the exact cached context");
    // Verify we never touched the OpenTelemetry propagators (Fast Path success!)
    verifyNoInteractions(otel);
  }

  @Test
  void shouldExtractFromHeadersAndCacheResultWhenNotAlreadyCached() {
    // Arrange: Simulate a raw request where OtelHttpTracing did NOT run
    when(joobyCtx.getAttribute(io.opentelemetry.context.Context.class.getName())).thenReturn(null);

    // Mock the OpenTelemetry propagator to return our fake extracted context.
    // STRICT MATCH: Ensure the first argument is strictly Context.root() to prevent thread-local
    // leakage.
    when(textMapPropagator.extract(
            eq(io.opentelemetry.context.Context.root()), eq(joobyCtx), any()))
        .thenReturn(otelCtx);

    // Act
    io.opentelemetry.context.Context result = extractor.extract(joobyCtx);

    // Assert
    assertSame(otelCtx, result, "Should return the context extracted from headers");

    // Verify it was explicitly called with the root context
    verify(textMapPropagator)
        .extract(eq(io.opentelemetry.context.Context.root()), eq(joobyCtx), any());

    // Verify the extractor cached it for the next time someone asks in this request lifecycle
    verify(joobyCtx).setAttribute(io.opentelemetry.context.Context.class.getName(), otelCtx);
  }

  @Test
  void joobyRequestGetterShouldReturnHeaderKeys() {
    // Arrange
    Map<String, String> fakeHeaders = Map.of("traceparent", "123", "tracestate", "456");
    when(joobyCtx.headerMap()).thenReturn(fakeHeaders);

    // Act
    Iterable<String> keys = DefaultOtelContextExtractor.Headers.INSTANCE.keys(joobyCtx);

    // Assert
    assertEquals(Set.of("traceparent", "tracestate"), keys);
  }

  @Test
  void joobyRequestGetterShouldReturnHeaderValueOrNull() {
    // Arrange
    Value mockHeaderValue = mock(Value.class);
    when(mockHeaderValue.valueOrNull())
        .thenReturn("00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01");
    when(joobyCtx.header("traceparent")).thenReturn(mockHeaderValue);

    // Act
    String headerVal = DefaultOtelContextExtractor.Headers.INSTANCE.get(joobyCtx, "traceparent");

    // Assert
    assertEquals("00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01", headerVal);
  }

  @Test
  void joobyRequestGetterShouldHandleMissingHeaderGracefully() {
    // Arrange
    Value mockMissingHeader = mock(Value.class);
    when(mockMissingHeader.valueOrNull()).thenReturn(null);
    when(joobyCtx.header("missing-header")).thenReturn(mockMissingHeader);

    // Act
    String headerVal = DefaultOtelContextExtractor.Headers.INSTANCE.get(joobyCtx, "missing-header");

    // Assert
    assertEquals(null, headerVal);
  }
}
