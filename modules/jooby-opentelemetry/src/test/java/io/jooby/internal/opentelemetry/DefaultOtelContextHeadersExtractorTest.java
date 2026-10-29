/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.opentelemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.jooby.Context;
import io.jooby.value.Value;
import io.jooby.value.ValueFactory;

class DefaultOtelContextHeadersExtractorTest {

  @Test
  void shouldExtractKeys() {
    var ctx = mock(io.jooby.Context.class);
    when(ctx.headerMap())
        .thenReturn(
            Map.of("traceparent", "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01"));
    var headers = DefaultOtelContextExtractor.Headers.INSTANCE;
    headers.keys(ctx);
  }

  @Test
  void shouldExtractHeaderValue() {
    var ctx = mock(io.jooby.Context.class);
    var headerValue = mock(io.jooby.value.Value.class);
    when(headerValue.toList())
        .thenReturn(java.util.List.of("00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01"));
    when(ctx.header("traceparent")).thenReturn(headerValue);
    var headers = DefaultOtelContextExtractor.Headers.INSTANCE;
    headers.get(ctx, "traceparent");
  }

  @Test
  void shouldExtractHeaderRealSingleValue() {
    var ctx = mock(io.jooby.Context.class);
    var firstValue = "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01";
    var headerValue = Value.value(new ValueFactory(), "traceparent", firstValue);
    when(ctx.header("traceparent")).thenReturn(headerValue);
    var headers = DefaultOtelContextExtractor.Headers.INSTANCE;
    assertEquals(firstValue, headers.get(ctx, "traceparent"));
  }

  @Test
  void shouldExtractHeaderRealListValue() {
    var ctx = mock(io.jooby.Context.class);
    var firstValue = "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01";
    var secondValue = "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-02";
    var headerValue =
        Value.array(new ValueFactory(), "traceparent", List.of(firstValue, secondValue));
    when(ctx.header("traceparent")).thenReturn(headerValue);
    var headers = DefaultOtelContextExtractor.Headers.INSTANCE;
    assertEquals(firstValue, headers.get(ctx, "traceparent"));
  }

  @Test
  void shouldExtractHeaderRealEmptyValue() {
    var ctx = mock(io.jooby.Context.class);
    var headerValue = Value.missing(new ValueFactory(), "traceparent");
    when(ctx.header("traceparent")).thenReturn(headerValue);
    var headers = DefaultOtelContextExtractor.Headers.INSTANCE;
    assertEquals(null, headers.get(ctx, "traceparent"));
  }

  @Test
  void shouldExtractHeaderAllValue() {
    var ctx = mock(Context.class);
    var headerValue = mock(Value.class);
    var values = List.of("00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01");
    when(headerValue.toList()).thenReturn(values);
    when(ctx.header("traceparent")).thenReturn(headerValue);
    var headers = DefaultOtelContextExtractor.Headers.INSTANCE;
    assertThat(headers.getAll(ctx, "traceparent")).toIterable().containsExactlyElementsOf(values);
  }
}
