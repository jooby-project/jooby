/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.opentelemetry.instrumentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.github.kagkarlsson.scheduler.event.ExecutionChain;
import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;

public class OtelDbSchedulerTest {

  @RegisterExtension
  static final OpenTelemetryExtension otelTesting = OpenTelemetryExtension.create();

  private TaskInstance<?> taskInstance;
  private ExecutionContext executionContext;
  private ExecutionChain chain;
  private CompletionHandler<?> completionHandler;

  private OtelDbScheduler interceptor;

  @BeforeEach
  void setUp() {
    otelTesting.clearSpans();
    otelTesting.clearMetrics();

    taskInstance = mock(TaskInstance.class);
    executionContext = mock(ExecutionContext.class);
    chain = mock(ExecutionChain.class);
    completionHandler = mock(CompletionHandler.class);

    when(taskInstance.getTaskName()).thenReturn("nightly-sync");
    when(taskInstance.getId()).thenReturn("sync-id-1234");

    // Initialize the interceptor using the in-memory OpenTelemetry SDK
    interceptor = new OtelDbScheduler(otelTesting.getOpenTelemetry());
  }

  @Test
  void shouldTraceAndRecordMetricsOnSuccess() {
    // Arrange
    when(chain.proceed(taskInstance, executionContext)).thenAnswer(invocation -> completionHandler);

    // Act
    Object result = interceptor.execute(taskInstance, executionContext, chain);

    // Assert Execution
    assertEquals(completionHandler, result);
    verify(chain).proceed(taskInstance, executionContext);

    // Assert Span
    List<SpanData> spans = otelTesting.getSpans();
    assertEquals(1, spans.size());
    SpanData span = spans.get(0);

    assertEquals("Job nightly-sync", span.getName());
    assertEquals(SpanKind.INTERNAL, span.getKind());
    assertEquals(StatusData.unset(), span.getStatus());
    assertThat(span.getAttributes().asMap())
        .containsEntry(AttributeKey.stringKey("job.system"), "db-scheduler")
        .containsEntry(AttributeKey.stringKey("job.id"), "sync-id-1234");

    // Assert Metrics (Counter)
    assertThat(otelTesting.getMetrics())
        .anySatisfy(
            metric -> {
              assertThat(metric.getName()).isEqualTo("dbscheduler.task.completions");
              assertThat(metric.getLongSumData().getPoints())
                  .anySatisfy(
                      point -> {
                        assertThat(point.getValue()).isEqualTo(1L);
                        assertThat(point.getAttributes().asMap())
                            .containsEntry(AttributeKey.stringKey("task"), "nightly-sync")
                            .containsEntry(AttributeKey.stringKey("result"), "ok");
                      });
            });

    // Assert Metrics (Histogram)
    assertThat(otelTesting.getMetrics())
        .anySatisfy(
            metric -> {
              assertThat(metric.getName()).isEqualTo("dbscheduler.task.duration");
              assertThat(metric.getHistogramData().getPoints())
                  .anySatisfy(
                      point -> {
                        assertThat(point.getCount()).isEqualTo(1L); // 1 recorded event
                        assertThat(point.getSum())
                            .isGreaterThanOrEqualTo(0.0); // duration in seconds
                        assertThat(point.getAttributes().asMap())
                            .containsEntry(AttributeKey.stringKey("task"), "nightly-sync")
                            .containsEntry(AttributeKey.stringKey("result"), "ok");
                      });
            });
  }

  @Test
  void shouldTraceAndRecordMetricsOnFailure() {
    // Arrange
    RuntimeException expectedException = new RuntimeException("Database timeout");
    when(chain.proceed(taskInstance, executionContext)).thenThrow(expectedException);

    // Act & Assert Exception
    assertThatThrownBy(() -> interceptor.execute(taskInstance, executionContext, chain))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Database timeout");

    // Assert Span
    List<SpanData> spans = otelTesting.getSpans();
    assertEquals(1, spans.size());
    SpanData span = spans.get(0);

    assertEquals("Job nightly-sync", span.getName());
    assertEquals(StatusData.create(StatusCode.ERROR, ""), span.getStatus());

    // Ensure exception was recorded as a span event
    assertEquals(1, span.getEvents().size());
    assertEquals("exception", span.getEvents().get(0).getName());

    // Assert Metrics (Counter marked as failed)
    assertThat(otelTesting.getMetrics())
        .anySatisfy(
            metric -> {
              assertThat(metric.getName()).isEqualTo("dbscheduler.task.completions");
              assertThat(metric.getLongSumData().getPoints())
                  .anySatisfy(
                      point -> {
                        assertThat(point.getValue()).isEqualTo(1L);
                        assertThat(point.getAttributes().asMap())
                            .containsEntry(AttributeKey.stringKey("task"), "nightly-sync")
                            .containsEntry(AttributeKey.stringKey("result"), "failed");
                      });
            });

    // Assert Metrics (Histogram recorded despite failure)
    assertThat(otelTesting.getMetrics())
        .anySatisfy(
            metric -> {
              assertThat(metric.getName()).isEqualTo("dbscheduler.task.duration");
              assertThat(metric.getHistogramData().getPoints())
                  .anySatisfy(
                      point -> {
                        assertThat(point.getCount()).isEqualTo(1L);
                        assertThat(point.getAttributes().asMap())
                            .containsEntry(AttributeKey.stringKey("task"), "nightly-sync")
                            .containsEntry(AttributeKey.stringKey("result"), "failed");
                      });
            });
  }
}
