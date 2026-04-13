/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.opentelemetry.instrumentation;

import com.zaxxer.hikari.HikariDataSource;
import io.jooby.Jooby;
import io.jooby.Reified;
import io.jooby.opentelemetry.OtelExtension;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.hikaricp.v3_0.HikariTelemetry;

/**
 * OpenTelemetry extension for HikariCP connection pools.
 *
 * <p>This extension automatically instruments all {@link HikariDataSource} instances registered
 * within the Jooby application, exporting critical connection pool metrics (such as active
 * connections, idle connections, and connection timeouts) to the OpenTelemetry backend.
 *
 * <h3>Required Dependency</h3>
 *
 * <p>To use this extension, you must add the official OpenTelemetry HikariCP instrumentation
 * library to your project's classpath:
 *
 * <pre>{@code
 * <dependency>
 * <groupId>io.opentelemetry.instrumentation</groupId>
 * <artifactId>opentelemetry-hikaricp-3.0</artifactId>
 * </dependency>
 * }</pre>
 *
 * <h3>Installation Order</h3>
 *
 * <p>Application installation order is critical. The {@code OtelModule} must be installed
 * <strong>first</strong>, followed by the {@code HikariModule}.
 *
 * <pre>{@code
 * {
 * // 1. Install OpenTelemetry with the Hikari extension FIRST
 * install(new OtelModule(new OtelHikari()));
 *
 * // 2. Install HikariModule NEXT
 * install(new HikariModule());
 * }
 * }</pre>
 *
 * <p><em>Lifecycle Note:</em> Although {@code OtelModule} is installed first, this extension defers
 * its execution to the application's {@code onStarting} lifecycle hook. This ensures that all data
 * sources configured by the subsequent {@code HikariModule} are fully initialized and available in
 * the service registry before the metrics tracker is applied.
 *
 * @since 4.3.1
 * @author edgar
 */
public class OtelHikari implements OtelExtension {

  @Override
  public void install(Jooby application, OpenTelemetry openTelemetry) {
    java.util.List<HikariDataSource> dataSources =
        application.require(Reified.list(HikariDataSource.class));
    var hikariTelemetry = HikariTelemetry.create(openTelemetry);

    // Apply the telemetry metrics tracker to every configured Hikari connection pool
    for (HikariDataSource dataSource : dataSources) {
      dataSource.setMetricsTrackerFactory(hikariTelemetry.createMetricsTrackerFactory());
    }
  }
}
