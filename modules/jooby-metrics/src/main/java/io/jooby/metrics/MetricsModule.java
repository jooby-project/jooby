/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.metrics;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Reporter;
import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.typesafe.config.Config;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.Router;
import io.jooby.ServiceKey;
import io.jooby.ServiceRegistry;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public class MetricsModule implements Extension {

  static final String CACHE_HEADER_NAME = "Cache-Control";
  static final String CACHE_HEADER_VALUE = "must-revalidate,no-cache,no-store";

  private final String pattern;

  private final List<Consumer<Router>> routes = new ArrayList<>();

  private final Map<String, Metric> metrics = new LinkedHashMap<>();
  private final Map<String, Class<? extends Metric>> metricClasses = new LinkedHashMap<>();

  private final Map<String, HealthCheck> healthChecks = new LinkedHashMap<>();
  private final Map<String, Class<? extends HealthCheck>> healthCheckClasses = new LinkedHashMap<>();

  private final Set<BiFunction<MetricRegistry, Config, Reporter>> reporters = new LinkedHashSet<>();

  private final MetricRegistry metricRegistry;
  private final HealthCheckRegistry healthCheckRegistry;

  /**
   * Creates a new {@link MetricsModule}.
   *
   * @param metricRegistry Use the given metricRegistry.
   * @param healthCheckRegistry Use the given healthCheckRegistry.
   * @param pattern A root pattern where to publish all the services. Default is: <code>/sys</code>.
   */
  public MetricsModule(final MetricRegistry metricRegistry,
      final HealthCheckRegistry healthCheckRegistry, final String pattern) {
    this.metricRegistry = requireNonNull(metricRegistry, "Metric registry is required.");
    this.healthCheckRegistry = requireNonNull(healthCheckRegistry, "Health check registry is required.");
    this.pattern = requireNonNull(pattern, "A pattern is required.");
  }

  /**
   * Creates a new {@link MetricsModule}.
   *
   * @param metricRegistry Use the given metricRegistry.
   * @param healthCheckRegistry Use the given healthCheckRegistry.
   */
  public MetricsModule(final MetricRegistry metricRegistry, final HealthCheckRegistry healthCheckRegistry) {
    this(metricRegistry, healthCheckRegistry, "/sys");
  }

  /**
   * Creates a new {@link MetricsModule}.
   *
   * @param metricRegistry Use the given metricRegistry.
   * @param pattern A root pattern where to publish all the services. Default is: <code>/sys</code>.
   */
  public MetricsModule(final MetricRegistry metricRegistry, final String pattern) {
    this(metricRegistry, new HealthCheckRegistry(), pattern);
  }

  /**
   * Creates a new {@link MetricsModule}.
   *
   * @param pattern A root pattern where to publish all the services. Default is: <code>/sys</code>.
   */
  public MetricsModule(final String pattern) {
    this(new MetricRegistry(), pattern);
  }

  /**
   * Creates a new {@link MetricsModule}. Services will be available at: <code>/sys</code>.
   *
   * @param healthCheckRegistry Use the given healthCheckRegistry.
   */
  public MetricsModule(final HealthCheckRegistry healthCheckRegistry) {
    this(new MetricRegistry(), healthCheckRegistry, "/sys");
  }

  /**
   * Creates a new {@link MetricsModule}. Services will be available at: <code>/sys</code>.
   *
   * @param metricRegistry Use the given metricRegistry.
   */
  public MetricsModule(final MetricRegistry metricRegistry) {
    this(metricRegistry, "/sys");
  }

  /**
   * Creates a new {@link MetricsModule}. Services will be available at: <code>/sys</code>.
   */
  public MetricsModule() {
    this("/sys");
  }

  /**
   * Append a simple ping handler that results in a <code>200</code> responses with a
   * <code>pong</code> body. See {@link PingHandler}
   *
   * @return This metrics module.
   */
  public MetricsModule ping() {
    routes.add(router -> router
        .get(this.pattern + "/ping", new PingHandler()));

    return this;
  }

  /**
   * Append a handler that prints thread states (a.k.a thread dump). See {@link ThreadDumpHandler}.
   *
   * @return This metrics module.
   */
  public MetricsModule threadDump() {
    routes.add(router -> router
        .get(this.pattern + "/thread-dump", new ThreadDumpHandler()));

    return this;
  }

  /**
   * Append a metric to the {@link MetricRegistry}, this call is identical to
   * {@link MetricRegistry#register(String, Metric)}.
   *
   * @param name Name of the metric.
   * @param metric A metric object
   * @return This metrics module.
   */
  public MetricsModule metric(final String name, final Metric metric) {
    metrics.put(name, metric);
    return this;
  }

  /**
   * Append a metric to the {@link MetricRegistry}. The metric will be resolved by Guice. This call
   * is identical to {@link MetricRegistry#register(String, Metric)}.
   *
   * @param name Name of the metric.
   * @param metric A metric object.
   * @param <M> Metric type.
   * @return This metrics module.
   */
  public <M extends Metric> MetricsModule metric(final String name, final Class<M> metric) {
    metricClasses.put(name, metric);
    return this;
  }

  /**
   * Append a health check to the {@link HealthCheckRegistry}. This call is identical to
   * {@link HealthCheckRegistry#register(String, HealthCheck)}.
   *
   * @param name Name of the check.
   * @param check A check object.
   * @return This metrics module.
   */
  public MetricsModule healthCheck(final String name, final HealthCheck check) {
    healthChecks.put(name, check);
    return this;
  }

  /**
   * Append a health check to the {@link HealthCheckRegistry}. The metric will be resolved by Guice.
   * This call is identical to {@link HealthCheckRegistry#register(String, HealthCheck)}.
   *
   * @param name Name of the check.
   * @param check A check object.
   * @param <H> {@link HealthCheck} type.
   * @return This metrics module.
   */
  public <H extends HealthCheck> MetricsModule healthCheck(final String name, final Class<H> check) {
    healthCheckClasses.put(name, check);
    return this;
  }

  /**
   * Append a {@link Reporter} to the {@link MetricRegistry}.
   *
   * @param callback Reporter callback.
   * @return This metrics module.
   */
  public MetricsModule reporter(final BiFunction<MetricRegistry, Config, Reporter> callback) {
    this.reporters.add(requireNonNull(callback, "Callback is required."));
    return this;
  }

  /**
   * Append a {@link Reporter} to the {@link MetricRegistry}.
   *
   * @param callback Reporter callback.
   * @return This metrics module.
   */
  public MetricsModule reporter(final Function<MetricRegistry, Reporter> callback) {
    return reporter((registry, conf) -> callback.apply(registry));
  }

  @Override
  public void install(@Nonnull Jooby application) {
    MetricHandler metricHandler = new MetricHandler();
    application.get(this.pattern + "/metrics", metricHandler);
    application.get(this.pattern + "/metrics/:type", metricHandler);
    application.get(this.pattern + "/healthcheck", new HealthCheckHandler());

    routes.forEach(r -> r.accept(application));

    ServiceRegistry registry = application.getServices();

    registry.putIfAbsent(MetricRegistry.class, metricRegistry);
    registry.putIfAbsent(HealthCheckRegistry.class, healthCheckRegistry);

    metrics.forEach(metricRegistry::register);
    healthChecks.forEach(healthCheckRegistry::register);

    final Set<Reporter> reporters = new HashSet<>();

    application.onStarted(() -> {
      metricClasses.forEach((name, clazz) -> metricRegistry.register(name, application.require(clazz)));
      healthCheckClasses.forEach((name, clazz) -> healthCheckRegistry.register(name, application.require(clazz)));

      Config config = application.getConfig();

      this.reporters.stream()
          .map(r -> r.apply(metricRegistry, config))
          .filter(Objects::nonNull)
          .forEachOrdered(reporters::add);
    });

    application.onStop(() -> reporters.forEach(r -> {
      try {
        r.close();
      } catch (IOException e) {
        application.getLog().error("close of {} resulted in error", r, e);
      }
    }));
  }
}
