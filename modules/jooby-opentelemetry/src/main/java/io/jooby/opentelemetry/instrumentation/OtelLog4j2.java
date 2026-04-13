/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.opentelemetry.instrumentation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;

import io.jooby.Jooby;
import io.jooby.opentelemetry.OtelExtension;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.log4j.appender.v2_17.OpenTelemetryAppender;

/**
 * OpenTelemetry extension for Log4j2.
 *
 * <p>This extension automatically instruments the Log4j2 logging framework by dynamically attaching
 * an {@link OpenTelemetryAppender} to the root logger. This ensures that all application logs are
 * seamlessly exported to your OpenTelemetry backend, automatically correlated with active trace and
 * span IDs.
 *
 * <h3>Required Dependency</h3>
 *
 * <p>To use this extension, you must add the official OpenTelemetry Log4j2 appender instrumentation
 * library to your project's classpath:
 *
 * <pre>{@code
 * <dependency>
 * <groupId>io.opentelemetry.instrumentation</groupId>
 * <artifactId>opentelemetry-log4j-appender-2.17</artifactId>
 * </dependency>
 * }</pre>
 *
 * <h3>Usage</h3>
 *
 * <p>Register this extension inside the core {@code OtelModule} during application setup:
 *
 * <pre>{@code
 * {
 * install(new OtelModule(
 * new OtelLog4j2()
 * ));
 * }
 * }</pre>
 *
 * <h3>Runtime Requirements</h3>
 *
 * <p>This extension requires {@code log4j-core} to be present at runtime to function correctly. It
 * accesses the underlying {@link LoggerContext} to dynamically inject the appender. If the
 * application is routing logs through a different backend (e.g., Logback or SimpleLogger), this
 * extension will gracefully fail and log a warning without crashing the application.
 *
 * @since 4.3.1
 * @author edgar
 */
public class OtelLog4j2 implements OtelExtension {

  @Override
  public void install(Jooby application, OpenTelemetry openTelemetry) {
    var currentContext = LogManager.getContext(application.getClassLoader(), false);

    if (currentContext instanceof LoggerContext loggerContext) {
      var config = loggerContext.getConfiguration();

      var otelAppender =
          OpenTelemetryAppender.builder()
              .setName("OpenTelemetry")
              .setOpenTelemetry(openTelemetry)
              .build();

      otelAppender.start();
      config.addAppender(otelAppender);

      config.getRootLogger().addAppender(otelAppender, null, null);
      loggerContext.updateLoggers();
    } else {
      application
          .getLog()
          .warn(
              "Log4j2OpenTelemetry requires log4j-core. Current context is: {}",
              currentContext.getClass().getName());
    }
  }
}
