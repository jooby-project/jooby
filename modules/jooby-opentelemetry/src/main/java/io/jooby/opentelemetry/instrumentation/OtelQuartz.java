/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.opentelemetry.instrumentation;

import org.quartz.Scheduler;

import io.jooby.Jooby;
import io.jooby.opentelemetry.OtelExtension;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.quartz.v2_0.QuartzTelemetry;

/**
 * OpenTelemetry extension for the Quartz scheduler.
 *
 * <p>This extension automatically instruments the Quartz {@link Scheduler} registered within the
 * Jooby application. It tracks the execution of all Quartz jobs, creating individual spans for each
 * execution to monitor scheduling delays, execution durations, and potential failures.
 *
 * <h3>Required Dependency</h3>
 *
 * <p>To use this extension, you must add the official OpenTelemetry Quartz instrumentation library
 * to your project's classpath:
 *
 * <pre>{@code
 * <dependency>
 * <groupId>io.opentelemetry.instrumentation</groupId>
 * <artifactId>opentelemetry-quartz-2.0</artifactId>
 * </dependency>
 * }</pre>
 *
 * <h3>Installation Order</h3>
 *
 * <p>The {@code OtelModule} should be installed alongside the Jooby {@code QuartzModule}.
 *
 * <pre>{@code
 * {
 * // 1. Install OpenTelemetry with the Quartz extension FIRST
 * install(new OtelModule(new OtelQuartz()));
 *
 * // 2. Install QuartzModule NEXT
 * install(new QuartzModule(MyJobs.class));
 * }
 * }</pre>
 *
 * <p><em>Lifecycle Note:</em> Although {@code OtelModule} is installed first, this extension defers
 * its execution to the application's {@code onStarting} lifecycle hook. This ensures that the
 * {@link Scheduler} configured by the {@code QuartzModule} is fully initialized and available in
 * the service registry before the OpenTelemetry listener is attached to it.
 *
 * @since 4.3.1
 * @author edgar
 */
public class OtelQuartz implements OtelExtension {

  @Override
  public void install(Jooby application, OpenTelemetry openTelemetry) throws Exception {
    var scheduler = application.require(Scheduler.class);

    // Build the official OTel listener
    var quartzTelemetry = QuartzTelemetry.builder(openTelemetry).build();
    quartzTelemetry.configure(scheduler);

    application.getLog().debug("OpenTelemetry Quartz JobListener installed.");
  }
}
