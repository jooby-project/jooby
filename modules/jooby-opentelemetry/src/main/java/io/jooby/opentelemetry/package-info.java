/**
 * OpenTelemetry module for Jooby.
 *
 * <p>This module integrates OpenTelemetry into your Jooby application, providing the foundational
 * engine for distributed tracing, metrics, and log correlation. It handles the lifecycle of the
 * {@link io.opentelemetry.api.OpenTelemetry} SDK and registers the SDK, the default {@link
 * io.opentelemetry.api.trace.Tracer}, and the fluent {@link io.jooby.opentelemetry.Trace} utility
 * into the Jooby application services.
 *
 * <h3>Important: Installation Order</h3>
 *
 * <p>Because this module bootstraps the core telemetry engine and registers the OpenTelemetry
 * instance into the application services, it <strong>must be installed at the very
 * beginning</strong> of your application setup. Installing it early ensures that all subsequent
 * routes, filters, and extensions have immediate access to the tracer and metric instruments.
 *
 * <h3>Usage</h3>
 *
 * <p>Install the module into your application, passing any specific OpenTelemetry extensions you
 * want to enable. To automatically trace HTTP requests, you must also append the {@code
 * OtelHttpTracing} filter to your routing pipeline:
 *
 * <pre>{@code
 * {
 * // 1. Install the core engine FIRST
 * install(new OtelModule(
 * new OtelLogback(),       // Injects Trace IDs into application logs
 * new OtelServerMetrics(), // Exports HTTP server metrics (e.g., Netty, Undertow, Jetty)
 * new OtelHikari()         // Traces database connection pools
 * ));
 *
 * // 2. Add the tracing filter to the routing pipeline
 * use(new OtelHttpTracing());
 *
 * // 3. Define routes
 * get("/books", ctx -> "List of books");
 * }
 * }</pre>
 *
 * <h3>Route Tracing (OtelHttpTracing)</h3>
 *
 * <p>While {@code OtelModule} bootstraps the core OpenTelemetry engine, it does not automatically
 * trace web requests. You must explicitly include {@code OtelHttpTracing}.
 *
 * <p>Note that {@code OtelHttpTracing} is <strong>not</strong> an {@link
 * io.jooby.opentelemetry.OtelExtension}; it is a native Jooby {@code Route.Filter}. It must be
 * installed directly into the application's routing pipeline (e.g., via {@code use()}) to
 * intercept, create, and propagate spans for incoming HTTP requests.
 *
 * <h3>Manual Tracing</h3>
 *
 * <p>For tracing specific business logic, database queries, or external API calls, this module
 * provides a fluent {@link io.jooby.opentelemetry.Trace} utility. You can retrieve it from the
 * route context or inject it directly into your service layer to safely create, configure, and
 * execute custom spans without risking context leaks.
 *
 * <pre>{@code
 * get("/books/{isbn}", ctx -> {
 *   Trace trace = ctx.require(Trace.class);
 *   String isbn = ctx.path("isbn").value();
 *   return trace.span("fetch_book")
 *        .attribute("isbn", isbn)
 *        .execute(span -> {
 *           span.addEvent("Executing database query");
 *           return repository.findByIsbn(isbn);
 *         });
 * });
 * }</pre>
 *
 * <h3>Configuration</h3>
 *
 * <p>The OpenTelemetry SDK is configured directly from your application's {@code application.conf}.
 * Any property defined inside the {@code otel} block is automatically extracted and used to
 * configure the underlying SDK components, such as exporters, protocols, and service attributes.
 *
 * <pre>{@code
 * otel {
 * service.name = "jooby-api"
 * traces.exporter = otlp
 * metrics.exporter = otlp
 * logs.exporter = otlp
 * exporter.otlp.protocol = grpc
 * exporter.otlp.endpoint = "http://localhost:4317"
 * }
 * }</pre>
 *
 * <p>If no {@code otel} configuration block is present in the application configuration, the module
 * will fall back to a baseline, default SDK.
 *
 * <h3>Extensions Lifecycle</h3>
 *
 * <p>Additional OpenTelemetry integrations (such as logging appenders or connection pool metrics)
 * are provided via {@link io.jooby.opentelemetry.OtelExtension} implementations. These extensions
 * are not executed immediately upon module installation.
 *
 * <p>Instead, the module defers their execution by registering them to the application's {@code
 * onStarting} lifecycle hook. This guarantees that the primary OpenTelemetry SDK is fully
 * constructed, configured, and registered before any secondary extensions attempt to hook into it
 * or emit telemetry data.
 *
 * @since 4.3.1
 * @author edgar
 */
@edu.umd.cs.findbugs.annotations.ReturnValuesAreNonnullByDefault
package io.jooby.opentelemetry;
