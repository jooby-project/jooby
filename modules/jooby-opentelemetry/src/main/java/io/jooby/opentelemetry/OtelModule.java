/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.opentelemetry;

import static io.jooby.SneakyThrows.throwingConsumer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.bridge.SLF4JBridgeHandler;

import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.internal.opentelemetry.DefaultOtelContextExtractor;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.instrumentation.runtimetelemetry.RuntimeTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import jakarta.inject.Provider;

/**
 * OpenTelemetry module for Jooby.
 *
 * <p>This module integrates OpenTelemetry into your Jooby application, providing the foundational
 * engine for distributed tracing, metrics, and log correlation. It handles the lifecycle of the
 * {@link OpenTelemetry} SDK and registers the SDK, the default {@link Tracer}, and the fluent
 * {@link Trace} utility into the Jooby application services.
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
 * <p>Note that {@code OtelHttpTracing} is <strong>not</strong> an {@link OtelExtension}; it is a
 * native Jooby {@code Route.Filter}. It must be installed directly into the application's routing
 * pipeline (e.g., via {@code use()}) to intercept, create, and propagate spans for incoming HTTP
 * requests.
 *
 * <h3>Manual Tracing</h3>
 *
 * <p>For tracing specific business logic, database queries, or external API calls, this module
 * provides a fluent {@link Trace} utility. You can retrieve it from the route context or inject it
 * directly into your service layer to safely create, configure, and execute custom spans without
 * risking context leaks.
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
 * are provided via {@link OtelExtension} implementations. These extensions are not executed
 * immediately upon module installation.
 *
 * <p>Instead, the module defers their execution by registering them to the application's {@code
 * onStarting} lifecycle hook. This guarantees that the primary OpenTelemetry SDK is fully
 * constructed, configured, and registered before any secondary extensions attempt to hook into it
 * or emit telemetry data.
 *
 * @since 4.3.1
 * @author edgar
 */
public class OtelModule implements Extension {

  static {
    SLF4JBridgeHandler.install();
  }

  private final OpenTelemetry openTelemetry;
  private final List<OtelExtension> extensions;

  /**
   * Creates a new OpenTelemetry module with a pre-configured OpenTelemetry instance.
   *
   * @param openTelemetry A pre-configured OpenTelemetry instance.
   * @param extensions Optional extensions (e.g., OtelLogback, OtelHikari).
   */
  public OtelModule(OpenTelemetry openTelemetry, OtelExtension... extensions) {
    this.openTelemetry = openTelemetry;
    this.extensions = List.of(extensions);
  }

  /**
   * Creates a new OpenTelemetry module. The SDK will be automatically configured based on the
   * application's {@code application.conf}.
   *
   * @param extensions Optional extensions (e.g., OtelLogback, OtelHikari).
   */
  public OtelModule(OtelExtension... extensions) {
    this(null, extensions);
  }

  @Override
  public void install(Jooby application) {
    var otel = getOrCreate(application);
    if (!isRunningInJoobyRun() && otel instanceof AutoCloseable closeableOtel) {
      // Close the OpenTelemetry instance when the application is stopped, and we are not running
      // in joobyRun.
      application.onStop(closeableOtel);
    }
    var tracer = otel.getTracer("io.jooby.opentelemetry");

    application.onStop(RuntimeTelemetry.create(otel));
    var services = application.getServices();
    services.put(OpenTelemetry.class, otel);
    services.put(Tracer.class, tracer);
    services.put(Trace.class, trace(tracer));
    services.putIfAbsent(OtelContextExtractor.class, new DefaultOtelContextExtractor(otel));

    application.onStarting(
        () -> extensions.forEach(throwingConsumer(ext -> ext.install(application, otel))));
  }

  private static Provider<Trace> trace(Tracer tracer) {
    return () -> new Trace(tracer);
  }

  boolean isRunningInJoobyRun() {
    return getClass()
        .getClassLoader()
        .getClass()
        .getName()
        .equals("org.jboss.modules.ModuleClassLoader");
  }

  private OpenTelemetry getOrCreate(Jooby application) {
    if (this.openTelemetry == null) {
      var appConfig = application.getConfig();
      Map<String, String> otelProperties = new HashMap<>();
      if (appConfig.hasPath("otel")) {
        var otelConfig = appConfig.getConfig("otel");
        otelConfig
            .entrySet()
            .forEach(
                entry -> {
                  String key = "otel." + entry.getKey();
                  String value = entry.getValue().unwrapped().toString();
                  otelProperties.put(key, value);
                });
        return safeCreateOnJoobyRun(
            () ->
                AutoConfiguredOpenTelemetrySdk.builder()
                    .addPropertiesSupplier(() -> otelProperties)
                    .disableShutdownHook()
                    .setResultAsGlobal()
                    .build()
                    .getOpenTelemetrySdk());
      } else {
        return safeCreateOnJoobyRun(() -> OpenTelemetrySdk.builder().buildAndRegisterGlobal());
      }
    }
    return this.openTelemetry;
  }

  private OpenTelemetry safeCreateOnJoobyRun(Supplier<OpenTelemetry> supplier) {
    try {
      return supplier.get();
    } catch (IllegalStateException ex) {
      if (isRunningInJoobyRun()) {
        return GlobalOpenTelemetry.get();
      }
      throw ex;
    }
  }
}
