/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.opentelemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;

public class TraceTest {

  @RegisterExtension
  static final OpenTelemetryExtension otelTesting = OpenTelemetryExtension.create();

  private Trace trace;

  @BeforeEach
  void setUp() {
    // Clear any spans from previous tests
    otelTesting.clearSpans();

    // Inject the in-memory tracer
    Tracer tracer = otelTesting.getOpenTelemetry().getTracer("test-tracer");
    trace = new Trace(tracer);
  }

  @Test
  void shouldExecuteSpanTaskAndReturnResult() throws Exception {
    // Arrange
    AttributeKey<String> customKey = AttributeKey.stringKey("custom.typed");

    // Act
    String result =
        trace
            .span("db_query")
            .attribute("str.key", "value")
            .attribute("long.key", 42L)
            .attribute("double.key", 3.14)
            .attribute("bool.key", true)
            .attribute(customKey, "typed-value")
            .kind(SpanKind.CLIENT)
            .rootContext()
            .execute(
                span -> {
                  span.addEvent("executing statement");
                  return "success";
                });

    // Assert Result
    assertEquals("success", result);

    // Assert Span Data
    java.util.List<SpanData> spans = otelTesting.getSpans();
    assertEquals(1, spans.size());
    SpanData spanData = spans.get(0);

    assertEquals("db_query", spanData.getName());
    assertEquals(SpanKind.CLIENT, spanData.getKind());
    assertFalse(
        spanData.getParentSpanContext().isValid(), "Span should have no parent (rootContext)");
    assertEquals(StatusData.unset(), spanData.getStatus());

    assertEquals(1, spanData.getEvents().size());
    assertEquals("executing statement", spanData.getEvents().get(0).getName());

    assertThat(spanData.getAttributes().asMap())
        .containsEntry(AttributeKey.stringKey("str.key"), "value")
        .containsEntry(AttributeKey.longKey("long.key"), 42L)
        .containsEntry(AttributeKey.doubleKey("double.key"), 3.14)
        .containsEntry(AttributeKey.booleanKey("bool.key"), true)
        .containsEntry(customKey, "typed-value");
  }

  @Test
  void shouldExecuteSpanRunnableAndCloseSafely() throws Exception {
    // Act
    trace
        .span("background_job")
        .run(
            span -> {
              span.addEvent("job started");
            });

    // Assert
    java.util.List<SpanData> spans = otelTesting.getSpans();
    assertEquals(1, spans.size());
    SpanData spanData = spans.get(0);

    assertEquals("background_job", spanData.getName());
    assertEquals(SpanKind.INTERNAL, spanData.getKind()); // Default OTel kind
    assertEquals(StatusData.unset(), spanData.getStatus());
  }

  @Test
  void shouldRecordExceptionAndFailSpanInTask() {
    // Act & Assert Exception Thrown
    assertThatThrownBy(
            () -> {
              trace
                  .span("failing_task")
                  .execute(
                      span -> {
                        throw new IllegalStateException("Database connection failed");
                      });
            })
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Database connection failed");

    // Assert Span Data
    java.util.List<SpanData> spans = otelTesting.getSpans();
    assertEquals(1, spans.size());
    SpanData spanData = spans.get(0);

    assertEquals("failing_task", spanData.getName());
    // Verifies status code and message were set correctly
    assertEquals(
        StatusData.create(StatusCode.ERROR, "Database connection failed"), spanData.getStatus());

    // Verifies recordException(t) was called
    assertEquals(1, spanData.getEvents().size());
    assertEquals("exception", spanData.getEvents().get(0).getName());
  }

  @Test
  void shouldRecordExceptionWithNullMessage() {
    // Act & Assert Exception Thrown
    assertThatThrownBy(
            () -> {
              trace
                  .span("npe_task")
                  .run(
                      (Trace.SpanRunnable)
                          span -> {
                            throw new NullPointerException(); // NPEs typically have a null message
                          });
            })
        .isInstanceOf(NullPointerException.class);

    // Assert Span Data
    java.util.List<SpanData> spans = otelTesting.getSpans();
    assertEquals(1, spans.size());
    SpanData spanData = spans.get(0);

    // Verifies fallback to class name when exception message is null
    assertEquals(
        StatusData.create(StatusCode.ERROR, "java.lang.NullPointerException"),
        spanData.getStatus());
  }

  @Test
  void shouldAllowUnderlyingConfigurationViaEscapeHatch() throws Exception {
    // Act
    trace
        .span("configured_task")
        .configure(builder -> builder.setAttribute("hatch.attr", "opened"))
        .run(
            span -> {
              // Do nothing
            });

    // Assert
    java.util.List<SpanData> spans = otelTesting.getSpans();
    assertEquals(1, spans.size());
    SpanData spanData = spans.get(0);

    assertThat(spanData.getAttributes().asMap())
        .containsEntry(AttributeKey.stringKey("hatch.attr"), "opened");
  }
}
