/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.opentelemetry;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;
import io.jooby.Jooby;
import io.jooby.ServiceRegistry;
import io.jooby.SneakyThrows;
import io.jooby.internal.opentelemetry.DefaultOtelContextExtractor;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import jakarta.inject.Provider;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class OtelModuleTest {

  @Mock private Jooby application;
  @Mock private ServiceRegistry services;
  @Mock private Config rootConfig;

  interface CloseableOpenTelemetry extends OpenTelemetry, AutoCloseable {}

  @BeforeEach
  void setUp() {
    GlobalOpenTelemetry.resetForTest();
    // FIX 1: Use lenient() to prevent UnnecessaryStubbingExceptions in tests that don't reach this
    lenient().when(application.getServices()).thenReturn(services);
  }

  @AfterEach
  void tearDown() {
    GlobalOpenTelemetry.resetForTest();
  }

  @Test
  void shouldRegisterProvidedOpenTelemetryInstance() {
    OpenTelemetry noopOtel = OpenTelemetry.noop();
    OtelModule module = new OtelModule(noopOtel);

    module.install(application);

    verify(services).put(OpenTelemetry.class, noopOtel);
    verify(services).put(eq(Tracer.class), any(Tracer.class));
    verify(services)
        .putIfAbsent(eq(OtelContextExtractor.class), any(DefaultOtelContextExtractor.class));

    ArgumentCaptor<Provider<Trace>> providerCaptor = ArgumentCaptor.forClass(Provider.class);
    verify(services).put(eq(Trace.class), providerCaptor.capture());
    assertNotNull(providerCaptor.getValue().get());
  }

  @Test
  void shouldEvaluateIsRunningInJoobyRunLocally() {
    OtelModule module = new OtelModule();
    assertFalse(module.isRunningInJoobyRun());
  }

  @Test
  void shouldRegisterCloseableOtelOnStopIfNotJoobyRun() {
    CloseableOpenTelemetry otel = mock(CloseableOpenTelemetry.class);
    when(otel.getTracer(anyString())).thenReturn(mock(Tracer.class));

    // FIX 2: Stub meterBuilder so RuntimeTelemetry.create(otel) doesn't throw a
    // NullPointerException
    when(otel.meterBuilder(anyString())).thenReturn(OpenTelemetry.noop().meterBuilder("test"));

    OtelModule module = spy(new OtelModule(otel));
    doReturn(false).when(module).isRunningInJoobyRun();

    module.install(application);

    verify(application, times(2)).onStop(any(AutoCloseable.class));
    verify(application).onStop(otel);
  }

  @Test
  void shouldNotRegisterCloseableOtelOnStopIfJoobyRun() {
    CloseableOpenTelemetry otel = mock(CloseableOpenTelemetry.class);
    when(otel.getTracer(anyString())).thenReturn(mock(Tracer.class));

    // FIX 2: Stub meterBuilder so RuntimeTelemetry.create(otel) doesn't throw a
    // NullPointerException
    when(otel.meterBuilder(anyString())).thenReturn(OpenTelemetry.noop().meterBuilder("test"));

    OtelModule module = spy(new OtelModule(otel));
    doReturn(true).when(module).isRunningInJoobyRun();

    module.install(application);

    verify(application, times(1)).onStop(any(AutoCloseable.class));
    verify(application, never()).onStop(otel);
  }

  @Test
  void shouldCreateDefaultSdkIfConfigIsMissing() {
    when(application.getConfig()).thenReturn(rootConfig);
    when(rootConfig.hasPath("otel")).thenReturn(false);

    OtelModule module = spy(new OtelModule());
    module.install(application);

    verify(services).put(eq(OpenTelemetry.class), any(OpenTelemetry.class));
  }

  @Test
  void shouldCreateAutoConfiguredSdkIfConfigIsPresent() {
    Config otelConfig = mock(Config.class);
    when(application.getConfig()).thenReturn(rootConfig);
    when(rootConfig.hasPath("otel")).thenReturn(true);
    when(rootConfig.getConfig("otel")).thenReturn(otelConfig);

    // FIX 3: Explicitly set exporters to "none" to override OTel's default "otlp"
    // behavior, which crashes if the dependencies aren't explicitly on the classpath.
    Set<Map.Entry<String, ConfigValue>> entries =
        Set.of(
            new AbstractMap.SimpleEntry<>(
                "service.name", ConfigValueFactory.fromAnyRef("test-service")),
            new AbstractMap.SimpleEntry<>(
                "metrics.exporter", ConfigValueFactory.fromAnyRef("none")),
            new AbstractMap.SimpleEntry<>("traces.exporter", ConfigValueFactory.fromAnyRef("none")),
            new AbstractMap.SimpleEntry<>("logs.exporter", ConfigValueFactory.fromAnyRef("none")));
    when(otelConfig.entrySet()).thenReturn(entries);

    OtelModule module = new OtelModule();
    module.install(application);

    verify(services).put(eq(OpenTelemetry.class), any(OpenTelemetry.class));
  }

  @Test
  void shouldReturnGlobalOpenTelemetryIfIllegalStateAndRunningInJoobyRun() {
    GlobalOpenTelemetry.set(OpenTelemetry.noop());

    when(application.getConfig()).thenReturn(rootConfig);
    when(rootConfig.hasPath("otel")).thenReturn(false);

    OtelModule module = spy(new OtelModule());
    doReturn(true).when(module).isRunningInJoobyRun();

    module.install(application);

    verify(services).put(eq(OpenTelemetry.class), any(OpenTelemetry.class));
  }

  @Test
  void shouldThrowIfIllegalStateAndNotRunningInJoobyRun() {
    GlobalOpenTelemetry.set(OpenTelemetry.noop());

    when(application.getConfig()).thenReturn(rootConfig);
    when(rootConfig.hasPath("otel")).thenReturn(false);

    OtelModule module = spy(new OtelModule());
    doReturn(false).when(module).isRunningInJoobyRun();

    assertThrows(IllegalStateException.class, () -> module.install(application));
  }

  @Test
  void shouldTriggerExtensionsOnStartingHook() throws Exception {
    OtelExtension mockExtension = mock(OtelExtension.class);
    OpenTelemetry noopOtel = OpenTelemetry.noop();
    OtelModule module = new OtelModule(noopOtel, mockExtension);

    ArgumentCaptor<SneakyThrows.Runnable> runnableCaptor =
        ArgumentCaptor.forClass(SneakyThrows.Runnable.class);
    when(application.onStarting(runnableCaptor.capture())).thenReturn(application);

    module.install(application);

    runnableCaptor.getValue().run();

    verify(mockExtension).install(application, noopOtel);
  }
}
