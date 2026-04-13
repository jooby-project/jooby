/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.opentelemetry.instrumentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.slf4j.Logger;

import io.jooby.Jooby;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.log4j.appender.v2_17.OpenTelemetryAppender;

public class OtelLog4j2Test {

  private Jooby application;
  private OpenTelemetry openTelemetry;
  private Logger appLogger;
  private MockedStatic<LogManager> mockedLogManager;

  @BeforeEach
  void setUp() {
    application = mock(Jooby.class);
    openTelemetry = mock(OpenTelemetry.class);
    appLogger = mock(Logger.class);

    when(application.getClassLoader()).thenReturn(Thread.currentThread().getContextClassLoader());
    when(application.getLog()).thenReturn(appLogger);

    // Intercept the static LogManager factory for this test thread
    mockedLogManager = mockStatic(LogManager.class);
  }

  @AfterEach
  void tearDown() {
    // Crucial: Always close static mocks to prevent them from leaking into other tests
    mockedLogManager.close();
  }

  @Test
  void shouldInstallAppenderWhenLog4jCoreIsPresent() {
    // Arrange
    LoggerContext loggerContext = mock(LoggerContext.class);
    Configuration configuration = mock(Configuration.class);
    LoggerConfig rootLoggerConfig = mock(LoggerConfig.class);

    when(loggerContext.getConfiguration()).thenReturn(configuration);
    when(configuration.getRootLogger()).thenReturn(rootLoggerConfig);

    // Force LogManager to return our mocked core context
    mockedLogManager
        .when(() -> LogManager.getContext(any(ClassLoader.class), anyBoolean()))
        .thenReturn(loggerContext);

    OtelLog4j2 extension = new OtelLog4j2();

    // Act
    extension.install(application, openTelemetry);

    // Assert Appender Registration
    ArgumentCaptor<OpenTelemetryAppender> appenderCaptor =
        ArgumentCaptor.forClass(OpenTelemetryAppender.class);

    // 1. Verify appender was added to the global config
    verify(configuration).addAppender(appenderCaptor.capture());
    OpenTelemetryAppender appender = appenderCaptor.getValue();
    assertNotNull(appender);
    assertEquals("OpenTelemetry", appender.getName());

    // 2. Verify appender was specifically attached to the Root Logger
    verify(rootLoggerConfig).addAppender(eq(appender), eq(null), eq(null));

    // 3. Verify Log4j2 was instructed to apply the changes
    verify(loggerContext).updateLoggers();
  }

  @Test
  void shouldLogWarningWhenLog4jCoreIsNotPresent() {
    // Arrange
    // Simulate a runtime where log4j-api is present, but routing to SimpleLogger instead of
    // log4j-core
    org.apache.logging.log4j.spi.LoggerContext simpleContext =
        mock(org.apache.logging.log4j.spi.LoggerContext.class);

    mockedLogManager
        .when(() -> LogManager.getContext(any(ClassLoader.class), anyBoolean()))
        .thenReturn(simpleContext);

    OtelLog4j2 extension = new OtelLog4j2();

    // Act
    extension.install(application, openTelemetry);

    // Assert
    verify(appLogger)
        .warn(
            "Log4j2OpenTelemetry requires log4j-core. Current context is: {}",
            simpleContext.getClass().getName());
  }
}
