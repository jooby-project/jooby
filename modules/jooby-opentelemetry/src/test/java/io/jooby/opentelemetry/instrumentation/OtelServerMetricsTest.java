/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.opentelemetry.instrumentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.xnio.XnioWorker;
import org.xnio.management.XnioWorkerMXBean;

import io.jooby.Jooby;
import io.jooby.Server;
import io.jooby.netty.NettyEventLoopGroup;
import io.netty.buffer.ByteBufAllocatorMetric;
import io.netty.buffer.ByteBufAllocatorMetricProvider;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.SingleThreadEventExecutor;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;

public class OtelServerMetricsTest {

  @RegisterExtension
  static final OpenTelemetryExtension otelTesting = OpenTelemetryExtension.create();

  private Jooby application;
  private Server server;
  private Logger appLogger;

  @BeforeEach
  void setUp() {
    otelTesting.clearMetrics();

    application = mock(Jooby.class);
    server = mock(Server.class);
    appLogger = mock(Logger.class);

    when(application.require(Server.class)).thenReturn(server);
    when(application.getLog()).thenReturn(appLogger);
  }

  @Test
  void shouldLogDebugWhenServerIsUnknown() {
    // Arrange
    when(server.getName()).thenReturn("tomcat");
    OtelServerMetrics extension = new OtelServerMetrics();

    // Act
    extension.install(application, otelTesting.getOpenTelemetry());

    // Assert
    verify(appLogger).debug("No specific OTel metrics mapped for server: {}", "tomcat");
    assertThat(otelTesting.getMetrics()).isEmpty();
  }

  @Test
  void shouldInstrumentJetty() {
    // Arrange
    when(server.getName()).thenReturn("jetty");

    org.eclipse.jetty.server.Server jettyServer = mock(org.eclipse.jetty.server.Server.class);
    QueuedThreadPool threadPool = mock(QueuedThreadPool.class);
    ServerConnector connector = mock(ServerConnector.class);

    when(application.require(org.eclipse.jetty.server.Server.class)).thenReturn(jettyServer);
    when(jettyServer.getThreadPool()).thenReturn(threadPool);
    when(jettyServer.getConnectors())
        .thenReturn(new org.eclipse.jetty.server.Connector[] {connector});

    // Mock Jetty Stats
    when(threadPool.getBusyThreads()).thenReturn(42);
    when(threadPool.getIdleThreads()).thenReturn(10);
    when(threadPool.getQueueSize()).thenReturn(5);
    when(connector.getConnectedEndPoints())
        .thenReturn(Collections.nCopies(100, null)); // Simulates 100 connections

    OtelServerMetrics extension = new OtelServerMetrics();

    // Act
    extension.install(application, otelTesting.getOpenTelemetry());

    // Assert (Fetching metrics triggers the async callbacks)
    assertGaugeValue("server.jetty.threads.active", 42.0);
    assertGaugeValue("server.jetty.threads.idle", 10.0);
    assertGaugeValue("server.jetty.queue.size", 5.0);
    assertGaugeValue("server.jetty.connections.active", 100.0);
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void shouldInstrumentNetty() {
    // Arrange
    when(server.getName()).thenReturn("netty");

    NettyEventLoopGroup nettyGroups = mock(NettyEventLoopGroup.class);
    when(application.require(NettyEventLoopGroup.class)).thenReturn(nettyGroups);

    // --- 1. Mock Event Loop Group ---
    EventLoopGroup eventLoopGroup = mock(EventLoopGroup.class);
    when(nettyGroups.eventLoop()).thenReturn(eventLoopGroup);

    SingleThreadEventExecutor eventLoopExecutor = mock(SingleThreadEventExecutor.class);
    when(eventLoopExecutor.pendingTasks()).thenReturn(15);
    // EventLoopGroup implements Iterable<EventExecutor>
    when(eventLoopGroup.iterator())
        .thenAnswer(i -> List.<EventExecutor>of(eventLoopExecutor).iterator());

    // --- 2. Mock Acceptor Group (Different from Event Loop) ---
    EventLoopGroup acceptorGroup = mock(EventLoopGroup.class);
    when(nettyGroups.acceptor()).thenReturn(acceptorGroup);

    SingleThreadEventExecutor acceptorExecutor = mock(SingleThreadEventExecutor.class);
    when(acceptorGroup.iterator())
        .thenAnswer(i -> List.<EventExecutor>of(acceptorExecutor).iterator());

    // --- 3. Mock Worker (Using ThreadPoolExecutor scenario) ---
    ThreadPoolExecutor workerPool = mock(ThreadPoolExecutor.class);
    when(workerPool.getActiveCount()).thenReturn(30);

    // Mock the queue directly instead of trying to instantiate it with generic classes
    BlockingQueue queue = mock(BlockingQueue.class);
    when(queue.size()).thenReturn(7);
    when(workerPool.getQueue()).thenReturn(queue);

    when(nettyGroups.worker()).thenReturn(workerPool);

    // --- 4. Mock ByteBufAllocator ---
    // It must implement both ByteBufAllocator and ByteBufAllocatorMetricProvider
    io.netty.buffer.ByteBufAllocator allocator =
        mock(
            io.netty.buffer.ByteBufAllocator.class,
            withSettings().extraInterfaces(ByteBufAllocatorMetricProvider.class));
    ByteBufAllocatorMetric allocatorMetric = mock(ByteBufAllocatorMetric.class);

    when(((ByteBufAllocatorMetricProvider) allocator).metric()).thenReturn(allocatorMetric);
    when(allocatorMetric.usedDirectMemory()).thenReturn(1024L);
    when(allocatorMetric.usedHeapMemory()).thenReturn(2048L);
    when(application.require(io.netty.buffer.ByteBufAllocator.class)).thenReturn(allocator);

    OtelServerMetrics extension = new OtelServerMetrics();

    // Act
    extension.install(application, otelTesting.getOpenTelemetry());

    // Assert
    assertGaugeValue("server.netty.eventloop.pending_tasks", 15.0);
    assertGaugeValue("server.netty.eventloop.count", 1.0);
    assertGaugeValue("server.netty.acceptor.count", 1.0);
    assertGaugeValue("server.netty.worker.threads.active", 30.0);
    assertGaugeValue("server.netty.worker.queue.size", 7.0);
    assertGaugeValue("server.netty.memory.direct_used", 1024.0);
    assertGaugeValue("server.netty.memory.heap_used", 2048.0);
  }

  @Test
  void shouldInstrumentUndertow() {
    // Arrange
    when(server.getName()).thenReturn("undertow");

    io.undertow.Undertow undertow = mock(io.undertow.Undertow.class);
    XnioWorker worker = mock(XnioWorker.class);
    XnioWorkerMXBean mxBean = mock(XnioWorkerMXBean.class);
    io.undertow.Undertow.ListenerInfo listenerInfo = mock(io.undertow.Undertow.ListenerInfo.class);
    io.undertow.server.ConnectorStatistics stats =
        mock(io.undertow.server.ConnectorStatistics.class);

    when(application.require(io.undertow.Undertow.class)).thenReturn(undertow);
    when(undertow.getWorker()).thenReturn(worker);
    when(worker.getMXBean()).thenReturn(mxBean);
    when(undertow.getListenerInfo()).thenReturn(List.of(listenerInfo));
    when(listenerInfo.getConnectorStatistics()).thenReturn(stats);

    // Mock Undertow Stats
    when(mxBean.getBusyWorkerThreadCount()).thenReturn(64);
    when(mxBean.getWorkerQueueSize()).thenReturn(12);
    when(mxBean.getIoThreadCount()).thenReturn(4);
    when(stats.getActiveConnections()).thenReturn(250L);

    OtelServerMetrics extension = new OtelServerMetrics();

    // Act
    extension.install(application, otelTesting.getOpenTelemetry());

    // Assert
    assertGaugeValue("server.undertow.worker.threads.active", 64.0);
    assertGaugeValue("server.undertow.worker.queue.size", 12.0);
    assertGaugeValue("server.undertow.eventloop.count", 4.0);
    assertGaugeValue("server.undertow.connections.active", 250.0);
  }

  /**
   * Helper method to locate a specific metric by name and assert its single DoubleGauge value.
   * OpenTelemetry builds metrics as Doubles by default unless ofLongs() is explicitly called.
   */
  private void assertGaugeValue(String metricName, double expectedValue) {
    assertThat(otelTesting.getMetrics())
        .anySatisfy(
            metric -> {
              assertThat(metric.getName()).isEqualTo(metricName);
              assertThat(metric.getDoubleGaugeData().getPoints())
                  .anySatisfy(
                      point -> {
                        assertThat(point.getValue()).isEqualTo(expectedValue);
                      });
            });
  }
}
