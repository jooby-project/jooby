/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.opentelemetry;

import static io.opentelemetry.context.Context.current;

import io.jooby.Context;
import io.jooby.Route;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.TextMapGetter;

/**
 * OpenTelemetry HTTP tracing filter for Jooby routes.
 *
 * <p>This filter intercepts incoming HTTP requests and automatically creates an OpenTelemetry
 * {@link SpanKind#SERVER} span for the request lifecycle. It acts as the primary entry point for
 * distributed tracing in the web layer.
 *
 * <h3>Features</h3>
 *
 * <ul>
 *   <li><strong>Distributed Context Extraction:</strong> Automatically extracts W3C Trace Context
 *       headers (e.g., {@code traceparent}) from incoming requests to continue existing traces
 *       spanning multiple microservices.
 *   <li><strong>Safe Span Naming:</strong> Uses the Jooby route pattern (e.g., {@code GET
 *       /api/users/{id}}) rather than the raw URI to prevent metric high-cardinality issues.
 *   <li><strong>Semantic Conventions:</strong> Automatically populates standard HTTP attributes
 *       ({@code http.request.method}, {@code http.response.status_code}, etc.).
 *   <li><strong>Asynchronous Safety:</strong> Ties the span closure to Jooby's {@code onComplete}
 *       hook, ensuring the span is accurately timed even if the route executes asynchronously.
 * </ul>
 *
 * <h3>Usage</h3>
 *
 * <p>Register this filter globally in your application using {@code use()} or {@code decorator()}.
 * It must be registered <em>after</em> {@link OtelModule} is installed.
 *
 * <pre>{@code
 * {
 * install(new OtelModule());
 * use(new OtelHttpTracing());
 * * get("/users/{id}", ctx -> "User " + ctx.path("id").value());
 * }
 * }</pre>
 *
 * @author edgar
 * @since 4.3.1
 */
public class OtelHttpTracing implements Route.Filter {

  /**
   * Intercepts the HTTP request to initialize, populate, and eventually close the OpenTelemetry
   * span.
   *
   * @param next The next handler in the routing chain.
   * @return A wrapped route handler containing the tracing logic.
   */
  @Override
  public Route.Handler apply(Route.Handler next) {
    return ctx -> {
      // Create a high-cardinality-safe span name: e.g., "GET /api/users/{id}"
      var spanName = ctx.getMethod() + " " + ctx.getRoute().getPattern();
      var tracer = ctx.require(Tracer.class);
      var otel = ctx.require(OpenTelemetry.class);
      var propagator = otel.getPropagators().getTextMapPropagator();

      var extractedContext = propagator.extract(current(), ctx, JoobyRequestGetter.INSTANCE);
      var span =
          tracer
              .spanBuilder(spanName)
              .setParent(extractedContext)
              .setSpanKind(SpanKind.SERVER)
              .setAttribute("http.request.method", ctx.getMethod())
              .setAttribute("url.path", ctx.getRequestPath())
              .setAttribute("http.route", ctx.getRoute().getPattern())
              .startSpan();

      // Ensure the span is ended ONLY when the HTTP response is fully complete
      ctx.onComplete(
          context -> {
            int statusCode = context.getResponseCode().value();
            span.setAttribute("http.response.status_code", statusCode);
            if (statusCode >= 500) {
              // Mark as error based on standard semantic conventions
              span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR);
            }
            span.end();
          });

      // Activate the span in the current thread scope
      try (var scope = span.makeCurrent()) {
        ctx.setAttribute("otel-span", span);

        return next.apply(ctx);
      } catch (Throwable t) {
        span.recordException(t);
        span.setAttribute("http.response.status_code", ctx.getRouter().errorCode(t).value());
        throw t;
      }
    };
  }

  /**
   * A bridge implementation allowing OpenTelemetry to extract distributed tracing headers directly
   * from a Jooby {@link Context}.
   */
  enum JoobyRequestGetter implements TextMapGetter<Context> {
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
