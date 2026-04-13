/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.opentelemetry.instrumentation;

import com.github.kagkarlsson.scheduler.event.ExecutionChain;
import com.github.kagkarlsson.scheduler.event.ExecutionInterceptor;
import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;

/**
 * OpenTelemetry instrumentation for the {@code db-scheduler} library.
 *
 * <p>This class implements {@link ExecutionInterceptor} to automatically generate traces and
 * metrics for every scheduled task execution.
 *
 * <h3>Traces</h3>
 *
 * <p>Creates an {@link SpanKind#INTERNAL} span named {@code Job <taskName>} for each execution. The
 * span includes the following attributes:
 *
 * <ul>
 *   <li>{@code job.system}: Always set to {@code "db-scheduler"}
 *   <li>{@code job.id}: The unique identifier of the task instance
 * </ul>
 *
 * Exceptions thrown during execution are recorded on the span, and the span status is set to {@link
 * StatusCode#ERROR}.
 *
 * <h3>Metrics</h3>
 *
 * <p>Records the following metrics under the {@code io.jooby.db-scheduler} meter:
 *
 * <ul>
 *   <li>{@code dbscheduler.task.completions} (Counter): Tracks total task executions.
 *   <li>{@code dbscheduler.task.duration} (Histogram): Tracks execution time in seconds.
 * </ul>
 *
 * Both metrics include the {@code task} name and the execution {@code result} (either {@code "ok"}
 * or {@code "failed"}) as attributes.
 *
 * <h3>Usage</h3>
 *
 * <pre>{@code
 * install(new OtelModule(...));
 *
 * install(new DbSchedulerModule()
 *    .withExecutionInterceptor(new OtelDbScheduler(require(OpenTelemetry.class)))
 * )
 * }</pre>
 *
 * @author edgar
 * @since 4.3.1
 */
public class OtelDbScheduler implements ExecutionInterceptor {
  private final Tracer tracer;
  private final LongCounter completionsCounter;
  private final DoubleHistogram durationHistogram;

  /**
   * Creates a new OpenTelemetry interceptor for db-scheduler.
   *
   * @param openTelemetry The fully configured OpenTelemetry instance used to extract the {@link
   *     Tracer} and {@link io.opentelemetry.api.metrics.Meter}.
   */
  public OtelDbScheduler(OpenTelemetry openTelemetry) {
    this.tracer = openTelemetry.getTracer("io.jooby.db-scheduler");
    var meter = openTelemetry.getMeter("io.jooby.db-scheduler");

    this.completionsCounter =
        meter
            .counterBuilder("dbscheduler.task.completions")
            .setDescription("Successes and failures by task")
            .setUnit("{completion}")
            .build();

    this.durationHistogram =
        meter
            .histogramBuilder("dbscheduler.task.duration")
            .setDescription("Duration of executions")
            .setUnit("s")
            .build();
  }

  /**
   * Intercepts the task execution to start a span, measure duration, and record metrics.
   *
   * @param taskInstance The instance of the task being executed.
   * @param executionContext The current execution context.
   * @param chain The execution chain to proceed.
   * @return The completion handler returned by the underlying task or chain.
   */
  @Override
  public CompletionHandler<?> execute(
      TaskInstance<?> taskInstance, ExecutionContext executionContext, ExecutionChain chain) {

    var taskName = taskInstance.getTaskName();
    var startTime = System.nanoTime();

    var span =
        tracer
            .spanBuilder("Job " + taskName)
            .setSpanKind(SpanKind.INTERNAL)
            .setAttribute("job.system", "db-scheduler")
            .setAttribute("job.id", taskInstance.getId())
            .startSpan();

    try (var scope = span.makeCurrent()) {
      var result = chain.proceed(taskInstance, executionContext);

      recordMetrics(taskName, startTime, "ok");
      return result;

    } catch (Throwable t) {
      span.recordException(t);
      span.setStatus(StatusCode.ERROR);

      recordMetrics(taskName, startTime, "failed");
      throw t;
    } finally {
      span.end();
    }
  }

  /**
   * Records the completion and duration metrics for the task execution.
   *
   * @param taskName The name of the executed task.
   * @param startTimeNanos The start time of the execution in nanoseconds.
   * @param result The outcome of the execution (e.g., "ok" or "failed").
   */
  private void recordMetrics(String taskName, long startTimeNanos, String result) {
    var durationSeconds = (System.nanoTime() - startTimeNanos) / 1_000_000_000.0;

    var attributes =
        Attributes.of(
            AttributeKey.stringKey("task"), taskName,
            AttributeKey.stringKey("result"), result);

    completionsCounter.add(1, attributes);
    durationHistogram.record(durationSeconds, attributes);
  }
}
