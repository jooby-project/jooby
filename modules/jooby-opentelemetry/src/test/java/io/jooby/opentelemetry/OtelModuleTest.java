/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.opentelemetry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.jooby.Jooby;
import io.jooby.ServiceRegistry;
import io.jooby.SneakyThrows;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;

@ExtendWith(MockitoExtension.class)
class OtelModuleTest {

  @Mock private Jooby application;

  @Mock private ServiceRegistry services;

  // 1. DO NOT use @Mock here. Use the official Noop implementation!
  private final OpenTelemetry openTelemetry = OpenTelemetry.noop();

  // 2. Extract the noop tracer so we can verify it gets registered
  private final Tracer tracer = openTelemetry.getTracer("io.jooby.opentelemetry");

  @BeforeEach
  void setUp() {
    // 3. We no longer need any MeterBuilder or Metric mocks.
    // The Noop implementation handles all of that safely under the hood.
    when(application.getServices()).thenReturn(services);
  }

  @Test
  @DisplayName("Should register OpenTelemetry and Tracer into Jooby services")
  void shouldRegisterServices() {
    OtelModule module = new OtelModule(openTelemetry);
    module.install(application);

    verify(services).put(OpenTelemetry.class, openTelemetry);
    verify(services).put(Tracer.class, tracer);
  }

  @Test
  @DisplayName("Should register RuntimeTelemetry onStop hook")
  void shouldRegisterOnStopHooks() {
    OtelModule module = new OtelModule(openTelemetry);
    module.install(application);

    // Verify that application.onStop is called with the RuntimeTelemetry auto-closeable
    verify(application).onStop(any(AutoCloseable.class));
  }

  @Test
  @DisplayName("Should trigger nested extensions on application start")
  void shouldTriggerExtensionsOnStarting() throws Exception {
    OtelExtension mockExtension = mock(OtelExtension.class);
    OtelModule module = new OtelModule(openTelemetry, mockExtension);

    // Capture the Runnable passed to application.onStarting
    ArgumentCaptor<SneakyThrows.Runnable> runnableCaptor =
        ArgumentCaptor.forClass(SneakyThrows.Runnable.class);
    when(application.onStarting(runnableCaptor.capture())).thenReturn(application);

    module.install(application);

    // Execute the captured Runnable (simulating Jooby starting)
    SneakyThrows.Runnable startingTask = runnableCaptor.getValue();
    startingTask.run();

    // Verify the nested extension was executed with the correct application and OTel instance
    verify(mockExtension).install(application, openTelemetry);
  }
}
