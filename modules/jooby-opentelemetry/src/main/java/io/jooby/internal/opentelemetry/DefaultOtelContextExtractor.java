/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.opentelemetry;

import static io.opentelemetry.context.Context.root;

import org.jspecify.annotations.NonNull;

import io.jooby.opentelemetry.OtelContextExtractor;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;

public class DefaultOtelContextExtractor implements OtelContextExtractor {

  private final OpenTelemetry otel;

  public DefaultOtelContextExtractor(OpenTelemetry otel) {
    this.otel = otel;
  }

  @Override
  public @NonNull Context extract(io.jooby.@NonNull Context ctx) {
    // 1. Primary: Check if the OtelHttpTracing middleware already saved it
    Context result = ctx.getAttribute(Context.class.getName());
    if (result == null) {
      // 2. Secondary: If middleware is missing, manually parse the W3C headers
      var propagator = otel.getPropagators().getTextMapPropagator();
      // Extracts W3C headers (if present) or returns Context.current() as a safe fallback
      result = propagator.extract(root(), ctx, Headers.INSTANCE);
      // Cache it to avoid re-parsing headers on subsequent calls in the same request
      ctx.setAttribute(Context.class.getName(), result);
    }
    return result;
  }

  /**
   * A bridge implementation allowing OpenTelemetry to extract distributed tracing headers directly
   * from a Jooby {@link io.jooby.Context}.
   */
  enum Headers implements TextMapGetter<io.jooby.Context> {
    INSTANCE;

    @Override
    public Iterable<String> keys(io.jooby.Context ctx) {
      // Allows OTel to iterate over all header names if needed
      return ctx.headerMap().keySet();
    }

    @Override
    public String get(io.jooby.Context ctx, String key) {
      // Safely extract the header value, returning null if it doesn't exist
      return ctx.header(key).valueOrNull();
    }
  }
}
