/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.metrics;

import static java.util.Objects.requireNonNull;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.Route;
import org.jooby.Route.Definition;
import org.jooby.internal.metrics.HealthCheckRegistryProvider;
import org.jooby.internal.metrics.MetricRegistryInitializer;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Reporter;
import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.inject.Binder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.typesafe.config.Config;

/**
 * <h1>metrics</h1>
 * <p>
 * Metrics provides a powerful toolkit of ways to measure the behavior of critical components in
 * your production environment.
 * </p>
 *
 * <h2>usage</h2>
 * <pre>
 * {
 *   use(new Metrics()
 *      .request()
 *      .threadDump()
 *      .ping()
 *      .healthCheck("db", new DatabaseHealthCheck())
 *      .metric("memory", new MemoryUsageGaugeSet())
 *      .metric("threads", new ThreadStatesGaugeSet())
 *      .metric("gc", new GarbageCollectorMetricSet())
 *      .metric("fs", new FileDescriptorRatioGauge())
 *      );
 * }
 * </pre>
 * <p>
 * Let's see what all these means.
 * </p>
 *
 * <h2>metrics</h2>
 * <p>
 * Metrics are available at <code>/sys/metrics</code> or <code>/sys/metrics/:type</code> via:
 * </p>
 * <pre>
 *   use(new Metrics()
 *      .metric("memory", new MemoryUsageGaugeSet())
 *      .metric("threads", new ThreadStatesGaugeSet())
 *      .metric("gc", new GarbageCollectorMetricSet())
 *      .metric("fs", new FileDescriptorRatioGauge()));
 * </pre>
 * <p>
 * The <code>/:type</code> parameter is optional and let you filter metrics by type
 * <code>counters</code>, <code>guages</code>, etc..
 * </p>
 * <p>
 * There is a <code>name</code> filter too: <code>/sys/metrics?name=memory</code> or
 * <code>/sys/metrics/guages?name=memory</code>. The <code>name</code> parameter filter all the
 * metrics where the name starts with the given <code>name</code>.
 * </p>
 *
 * <h2>health checks</h2>
 * <p>
 * Health checks are available at <code>/sys/healthCheck</code> via:
 * </p>
 * <pre>
 *   use(new Metrics()
 *      .healthCheck("db", new DatabaseHealthCheck()));
 * </pre>
 *
 * <h2>instrumented requests</h2>
 * <p>
 * Captures request information (like active requests or min/mean/max execution time) and a
 * breakdown of the response codes being returned: {@link InstrumentedHandler}.
 * </p>
 * <pre>
 *   use(new Metrics()
 *      .request());
 * </pre>
 *
 * <h2>thread dump</h2>
 * <p>
 * A thread dump is available at <code>/sys/threadDump</code> via:
 * </p>
 * <pre>
 *   use(new Metrics()
 *      .threadDump());
 * </pre>
 *
 * <h2>reporting</h2>
 * <p>
 * Reporters are appended via a callback API:
 * </p>
 * <pre>
 *   {
 *     use(new Metrics()
 *        .reporter(registry {@literal ->} {
 *          ConsoleReporter reporter = ConsoleReporter.forRegistry(registry).build();
 *          reporter.start(1, TimeUnit.MINUTES);
 *          return reporter;
 *        });
 *   }
 * </pre>
 * <p>
 * You can add all the reporters you want. Keep in mind you have to start them (if need it), but you
 * don't have to stop them as long they implements the {@link Closeable} interface.
 * </p>
 *
 * <p>That's all folks!</p>
 *
 * @author edgar
 * @since 0.13.0
 */
public class Metrics implements Jooby.Module {

  private String pattern;

  private List<BiConsumer<Binder, Config>> bindings = new ArrayList<>();

  private List<Route.Definition> routes = new ArrayList<>();

  private Set<BiFunction<MetricRegistry, Config, Reporter>> reporters = new LinkedHashSet<>();

  private MetricRegistry registry;

  /**
   * Creates a new {@link Metric} module.
   *
   * @param registry Use the given registry.
   * @param pattern A root pattern where to publish all the services. Default is: <code>/sys</code>.
   */
  public Metrics(final MetricRegistry registry, final String pattern) {
    this.registry = requireNonNull(registry, "Registry is required.");
    this.pattern = requireNonNull(pattern, "A pattern is required.");
  }

  /**
   * Creates a new {@link Metric} module.
   *
   * @param pattern A root pattern where to publish all the services. Default is: <code>/sys</code>.
   */
  public Metrics(final String pattern) {
    this(new MetricRegistry(), pattern);
  }

  /**
   * Creates a new {@link Metric} module. Services will be available at: <code>/sys</code>.
   *
   * @param registry Use the given registry.
   */
  public Metrics(final MetricRegistry registry) {
    this(registry, "/sys");
  }

  /**
   * Creates a new {@link Metric} module. Services will be available at: <code>/sys</code>.
   */
  public Metrics() {
    this("/sys");
  }

  /**
   * Instrument request using {@link InstrumentedHandler}.
   *
   * @param method Method to filter for. Default is: <code>GET</code>.
   * @param pattern A pattern to filter for. Default is: <code>*</code> (all the requests).
   * @return This metrics module.
   */
  public Metrics request(final String method, final String pattern) {
    routes.add(
        new Route.Definition(method, pattern, new InstrumentedHandler()));
    return this;
  }

  /**
   * Instrument request using {@link InstrumentedHandler}.
   *
   * @param pattern A pattern to filter for. Default is: <code>*</code> (all the requests).
   * @return This metrics module.
   */
  public Metrics request(final String pattern) {
    return request("GET", "*");
  }

  /**
   * Instrument request using {@link InstrumentedHandler}. It will intercept all the
   * <code>GET</code> calls.
   *
   * @return This metrics module.
   */
  public Metrics request() {
    return request("*");
  }

  /**
   * Append a simple ping handler that results in a <code>200</code> responses with a
   * <code>pong</code> body.
   *
   * @return This metrics module.
   * @see PingHandler.
   */
  public Metrics ping() {
    bindings.add((binder, conf) -> {
      Multibinder.newSetBinder(binder, Route.Definition.class).addBinding()
          .toInstance(new Route.Definition("GET", this.pattern + "/ping", new PingHandler()));
    });
    return this;
  }

  /**
   * Append a handler that prints thread states (a.k.a thread dump).
   *
   * @return This metrics module.
   * @see ThreadDumpHandler.
   */
  public Metrics threadDump() {
    bindings.add((binder, conf) -> {
      Multibinder<Definition> routes = Multibinder.newSetBinder(binder, Route.Definition.class);
      routes.addBinding().toInstance(
          new Route.Definition("GET", this.pattern + "/threadDump", new ThreadDumpHandler()));
    });
    return this;
  }

  /**
   * Append a metric to the {@link MetricRegistry}, this call is identical to
   * {@link MetricRegistry#register(String, Metric)}.
   *
   * @param name Name of the metric.
   * @param metric A metric object.
   * @return This metrics module.
   */
  public Metrics metric(final String name, final Metric metric) {
    bindings.add((binder, conf) -> {
      MapBinder.newMapBinder(binder, String.class, Metric.class).addBinding(name)
          .toInstance(metric);
    });
    return this;
  }

  /**
   * Append a metric to the {@link MetricRegistry}. The metric will be resolved by Guice. This call
   * is identical to {@link MetricRegistry#register(String, Metric)}.
   *
   * @param name Name of the metric.
   * @param metric A metric object.
   * @return This metrics module.
   */
  public <M extends Metric> Metrics metric(final String name, final Class<M> metric) {
    bindings.add((binder, conf) -> {
      MapBinder.newMapBinder(binder, String.class, Metric.class).addBinding(name)
          .to(metric);
    });
    return this;
  }

  /**
   * Append a health check to the {@link HealthCheckRegistry}. This call is identical to
   * {@link HealthCheckRegistry#register(String, HealthCheck)}.
   *
   * @param name Name of the check.
   * @param metric A check object.
   * @return This metrics module.
   */
  public Metrics healthCheck(final String name, final HealthCheck check) {
    bindings.add((binder, conf) -> {
      MapBinder.newMapBinder(binder, String.class, HealthCheck.class).addBinding(name)
          .toInstance(check);
    });
    return this;
  }

  /**
   * Append a health check to the {@link HealthCheckRegistry}. The metric will be resolved by Guice.
   * This call is identical to {@link HealthCheckRegistry#register(String, HealthCheck)}.
   *
   * @param name Name of the check.
   * @param metric A check object.
   * @return This metrics module.
   */
  public <H extends HealthCheck> Metrics healthCheck(final String name, final Class<H> check) {
    bindings.add((binder, conf) -> {
      MapBinder.newMapBinder(binder, String.class, HealthCheck.class)
          .addBinding(name)
          .to(check);
    });
    return this;
  }

  /**
   * Append a {@link Reporter} to the {@link MetricRegistry}.
   *
   * @param callback Reporter callback.
   * @return This metrics module.
   */
  public Metrics reporter(final BiFunction<MetricRegistry, Config, Reporter> callback) {
    this.reporters.add(requireNonNull(callback, "Callback is required."));
    return this;
  }

  /**
   * Append a {@link Reporter} to the {@link MetricRegistry}.
   *
   * @param callback Reporter callback.
   * @return This metrics module.
   */
  public Metrics reporter(final Function<MetricRegistry, Reporter> callback) {
    return reporter((registry, conf) -> callback.apply(registry));
  }

  @Override
  public void configure(final Env env, final Config conf, final Binder binder) {
    // empty metric & checks
    MapBinder.newMapBinder(binder, String.class, Metric.class);
    MapBinder.newMapBinder(binder, String.class, HealthCheck.class);

    Multibinder<Route.Definition> routes = Multibinder.newSetBinder(binder, Route.Definition.class);

    MetricHandler mhandler = new MetricHandler();
    routes.addBinding()
        .toInstance(new Route.Definition("GET", this.pattern + "/metrics", mhandler));
    routes.addBinding()
        .toInstance(new Route.Definition("GET", this.pattern + "/metrics/:type", mhandler));

    routes.addBinding().toInstance(
        new Route.Definition("GET", this.pattern + "/healthcheck", new HealthCheckHandler()));

    Multibinder<Reporter> reporters = Multibinder.newSetBinder(binder, Reporter.class);

    binder.bind(MetricRegistry.class).toInstance(registry);

    this.reporters.forEach(it -> reporters.addBinding().toInstance(it.apply(registry, conf)));

    binder.bind(MetricRegistryInitializer.class).asEagerSingleton();
    binder.bind(HealthCheckRegistry.class)
        .toProvider(HealthCheckRegistryProvider.class)
        .asEagerSingleton();

    bindings.forEach(it -> it.accept(binder, conf));

    this.routes.forEach(it -> routes.addBinding().toInstance(it));
  }

}
