/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.opentelemetry;

import java.util.function.Consumer;

import io.jooby.SneakyThrows;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;

/**
 * Injectable utility for creating safe OpenTelemetry traces and spans.
 *
 * @author edgar
 * @since 4.3.1
 */
public class Trace {

  private final Tracer tracer;

  public Trace(Tracer tracer) {
    this.tracer = tracer;
  }

  /**
   * Begins building a new OpenTelemetry span operation.
   *
   * @param name The name of the operation.
   * @return A fluent builder to add attributes and execute logic.
   */
  public Operation span(String name) {
    return new Operation(tracer, name);
  }

  public interface SpanTask<T> {
    T execute(Span span) throws Exception;
  }

  public interface SpanRunnable {
    void run(Span span) throws Exception;
  }

  /** Represents an in-flight trace operation. */
  public static class Operation {
    private final io.opentelemetry.api.trace.SpanBuilder otelSpanBuilder;

    private Operation(Tracer tracer, String name) {
      this.otelSpanBuilder = tracer.spanBuilder(name);
    }

    /** Escape hatch: Provides direct access to the native OpenTelemetry SpanBuilder. */
    public Operation configure(Consumer<io.opentelemetry.api.trace.SpanBuilder> customizer) {
      customizer.accept(otelSpanBuilder);
      return this;
    }

    public Operation attribute(String key, String value) {
      otelSpanBuilder.setAttribute(key, value);
      return this;
    }

    public Operation attribute(String key, long value) {
      otelSpanBuilder.setAttribute(key, value);
      return this;
    }

    public Operation attribute(String key, double value) {
      otelSpanBuilder.setAttribute(key, value);
      return this;
    }

    public Operation attribute(String key, boolean value) {
      otelSpanBuilder.setAttribute(key, value);
      return this;
    }

    /** Supports strongly-typed OpenTelemetry semantic convention keys. */
    public <T> Operation attribute(AttributeKey<T> key, T value) {
      otelSpanBuilder.setAttribute(key, value);
      return this;
    }

    public Operation kind(io.opentelemetry.api.trace.SpanKind kind) {
      otelSpanBuilder.setSpanKind(kind);
      return this;
    }

    public Operation rootContext() {
      otelSpanBuilder.setNoParent();
      return this;
    }

    /** Executes logic that returns a value within the span context. */
    public <T> T execute(SpanTask<T> block) {
      var span = otelSpanBuilder.startSpan();
      try (var scope = span.makeCurrent()) {
        return block.execute(span);
      } catch (Throwable t) {
        span.recordException(t);
        span.setStatus(
            StatusCode.ERROR, t.getMessage() != null ? t.getMessage() : t.getClass().getName());
        throw SneakyThrows.propagate(t);
      } finally {
        span.end();
      }
    }

    /** Executes void logic within the span context. */
    public void run(SpanRunnable block) {
      var span = otelSpanBuilder.startSpan();
      try (var scope = span.makeCurrent()) {
        block.run(span);
      } catch (Throwable t) {
        span.recordException(t);
        span.setStatus(
            StatusCode.ERROR, t.getMessage() != null ? t.getMessage() : t.getClass().getName());
        throw SneakyThrows.propagate(t);
      } finally {
        span.end();
      }
    }
  }
}
