/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.opentelemetry.instrumentation;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.OngoingStubbing;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.MetricsTrackerFactory;
import io.jooby.Jooby;
import io.jooby.Reified;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;

public class OtelHikariTest {

  @RegisterExtension
  static final OpenTelemetryExtension otelTesting = OpenTelemetryExtension.create();

  private Jooby application;
  private HikariDataSource primaryDataSource;
  private HikariDataSource secondaryDataSource;

  @BeforeEach
  void setUp() {
    application = mock(Jooby.class);
    primaryDataSource = mock(HikariDataSource.class);
    secondaryDataSource = mock(HikariDataSource.class);
  }

  @Test
  void shouldInstrumentAllConfiguredDataSources() {
    // Arrange
    // Simulate a Jooby application with two separate database connections
    List<HikariDataSource> dataSources = Arrays.asList(primaryDataSource, secondaryDataSource);

    // Mock Jooby's Reified list resolution
    OngoingStubbing<List<HikariDataSource>> when =
        when(application.require(Reified.list(HikariDataSource.class)));
    when.thenReturn(dataSources);

    OtelHikari extension = new OtelHikari();

    // Act
    extension.install(application, otelTesting.getOpenTelemetry());

    // Assert primary data source was instrumented
    ArgumentCaptor<MetricsTrackerFactory> captor1 =
        ArgumentCaptor.forClass(MetricsTrackerFactory.class);
    verify(primaryDataSource).setMetricsTrackerFactory(captor1.capture());
    assertNotNull(
        captor1.getValue(), "MetricsTrackerFactory should be applied to primary data source");

    // Assert secondary data source was instrumented
    ArgumentCaptor<MetricsTrackerFactory> captor2 =
        ArgumentCaptor.forClass(MetricsTrackerFactory.class);
    verify(secondaryDataSource).setMetricsTrackerFactory(captor2.capture());
    assertNotNull(
        captor2.getValue(), "MetricsTrackerFactory should be applied to secondary data source");
  }
}
