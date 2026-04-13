/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.opentelemetry.instrumentation;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.quartz.ListenerManager;
import org.quartz.Scheduler;
import org.slf4j.Logger;

import io.jooby.Jooby;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;

public class OtelQuartzTest {

  @RegisterExtension
  static final OpenTelemetryExtension otelTesting = OpenTelemetryExtension.create();

  private Jooby application;
  private Scheduler scheduler;
  private ListenerManager listenerManager;
  private Logger appLogger;

  @BeforeEach
  void setUp() throws Exception {
    application = mock(Jooby.class);
    scheduler = mock(Scheduler.class);
    listenerManager = mock(ListenerManager.class);
    appLogger = mock(Logger.class);

    // Mock Jooby's registry lookup
    when(application.require(Scheduler.class)).thenReturn(scheduler);
    when(application.getLog()).thenReturn(appLogger);

    // OTel's QuartzTelemetry requires the ListenerManager to attach its JobListener.
    // If we don't mock this, quartzTelemetry.configure(scheduler) will throw an NPE.
    when(scheduler.getListenerManager()).thenReturn(listenerManager);
  }

  @Test
  void shouldInstallQuartzTelemetryListener() throws Exception {
    // Arrange
    OtelQuartz extension = new OtelQuartz();

    // Act
    extension.install(application, otelTesting.getOpenTelemetry());

    // Assert
    // 1. Verify we requested the Scheduler from Jooby
    verify(application).require(Scheduler.class);

    // 2. Verify OpenTelemetry actually interacted with the Quartz Scheduler to hook its listener
    verify(scheduler, times(2)).getListenerManager();

    // 3. Verify our success debug log was fired
    verify(appLogger).debug("OpenTelemetry Quartz JobListener installed.");
  }
}
