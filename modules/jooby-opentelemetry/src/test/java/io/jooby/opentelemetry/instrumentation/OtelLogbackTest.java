/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.opentelemetry.instrumentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.jooby.Jooby;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;

public class OtelLogbackTest {

  private Jooby application;
  private OpenTelemetry openTelemetry;
  private org.slf4j.Logger appLogger;
  private MockedStatic<LoggerFactory> mockedLoggerFactory;

  @BeforeEach
  void setUp() {
    application = mock(Jooby.class);
    openTelemetry = mock(OpenTelemetry.class);
    appLogger = mock(org.slf4j.Logger.class);

    when(application.getLog()).thenReturn(appLogger);

    // Intercept the static SLF4J LoggerFactory for this test thread
    mockedLoggerFactory = mockStatic(LoggerFactory.class);
  }

  @AfterEach
  void tearDown() {
    // Crucial: Always close static mocks to prevent them from breaking the test runner's own
    // logging
    mockedLoggerFactory.close();
  }

  @Test
  void shouldInstallAppenderWhenLogbackIsPresent() {
    // Arrange
    LoggerContext loggerContext = mock(LoggerContext.class);
    Logger rootLogger = mock(Logger.class);

    // Make the factory return our Logback context
    mockedLoggerFactory.when(LoggerFactory::getILoggerFactory).thenReturn(loggerContext);

    // Wire up the root logger retrieval
    when(loggerContext.getLogger(Logger.ROOT_LOGGER_NAME)).thenReturn(rootLogger);

    OtelLogback extension = new OtelLogback();

    // Act
    extension.install(application, openTelemetry);

    // Assert Appender Registration
    ArgumentCaptor<OpenTelemetryAppender> appenderCaptor =
        ArgumentCaptor.forClass(OpenTelemetryAppender.class);

    verify(rootLogger).addAppender(appenderCaptor.capture());

    OpenTelemetryAppender appender = appenderCaptor.getValue();
    assertEquals("OpenTelemetry", appender.getName());
    assertTrue(
        appender.isStarted(), "The OpenTelemetryAppender should be started before being attached");
  }

  @Test
  void shouldLogWarningWhenLogbackIsNotPresent() {
    // Arrange
    // Simulate an environment using a different SLF4J binding (like slf4j-simple)
    ILoggerFactory simpleFactory = mock(ILoggerFactory.class);
    mockedLoggerFactory.when(LoggerFactory::getILoggerFactory).thenReturn(simpleFactory);

    OtelLogback extension = new OtelLogback();

    // Act
    extension.install(application, openTelemetry);

    // Assert
    verify(appLogger)
        .warn(
            "LogbackOpenTelemetry requires Logback. Current factory: {}",
            simpleFactory.getClass().getName());
  }
}
