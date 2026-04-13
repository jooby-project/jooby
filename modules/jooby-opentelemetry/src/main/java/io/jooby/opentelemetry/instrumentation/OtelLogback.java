/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.opentelemetry.instrumentation;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.jooby.Jooby;
import io.jooby.opentelemetry.OtelExtension;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;

/**
 * OpenTelemetry extension for Logback.
 *
 * <p>This extension automatically instruments the Logback logging framework by dynamically
 * attaching an {@link OpenTelemetryAppender} to the root logger. This ensures that all application
 * logs are seamlessly exported to your OpenTelemetry backend, automatically correlated with active
 * trace and span IDs.
 *
 * <h3>Required Dependency</h3>
 *
 * <p>To use this extension, you must add the official OpenTelemetry Logback appender
 * instrumentation library to your project's classpath:
 *
 * <pre>{@code
 * <dependency>
 * <groupId>io.opentelemetry.instrumentation</groupId>
 * <artifactId>opentelemetry-logback-appender-1.0</artifactId>
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
 * new OtelLogback()
 * ));
 * }
 * }</pre>
 *
 * <h3>Runtime Requirements</h3>
 *
 * <p>This extension requires Logback to be the active SLF4J binding at runtime. It verifies that
 * the underlying factory is a {@link LoggerContext} before injecting the appender. If the
 * application routes logs through a different backend (e.g., SimpleLogger, Log4j2), this extension
 * will safely bypass installation and log a warning.
 *
 * @since 4.3.1
 * @author edgar
 */
public class OtelLogback implements OtelExtension {

  @Override
  public void install(Jooby application, OpenTelemetry openTelemetry) {
    var loggerFactory = LoggerFactory.getILoggerFactory();

    // Ensure we are actually running Logback before casting
    if (loggerFactory instanceof LoggerContext loggerContext) {
      var otelAppender = new OpenTelemetryAppender();
      otelAppender.setName("OpenTelemetry");
      otelAppender.setContext(loggerContext);
      otelAppender.setOpenTelemetry(openTelemetry);

      // Start the appender
      otelAppender.start();

      // Attach it to the Root Logger so it catches everything
      var rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
      rootLogger.addAppender(otelAppender);
    } else {
      application
          .getLog()
          .warn(
              "LogbackOpenTelemetry requires Logback. Current factory: {}",
              loggerFactory.getClass().getName());
    }
  }
}
