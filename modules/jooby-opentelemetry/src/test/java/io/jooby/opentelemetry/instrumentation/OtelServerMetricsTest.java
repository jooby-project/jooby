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
    when(server.getName()).thenReturn("tomcat");
    OtelServerMetrics extension = new OtelServerMetrics();

    extension.install(application, otelTesting.getOpenTelemetry());

    verify(appLogger).debug("No specific OTel metrics mapped for server: {}", "tomcat");
    assertThat(otelTesting.getMetrics()).isEmpty();
  }

  @Test
  void shouldInstrumentJetty() {
    when(server.getName()).thenReturn("jetty");

    org.eclipse.jetty.server.Server jettyServer = mock(org.eclipse.jetty.server.Server.class);
    QueuedThreadPool threadPool = mock(QueuedThreadPool.class);
    ServerConnector connector = mock(ServerConnector.class);

    when(application.require(org.eclipse.jetty.server.Server.class)).thenReturn(jettyServer);
    when(jettyServer.getThreadPool()).thenReturn(threadPool);
    when(jettyServer.getConnectors())
        .thenReturn(new org.eclipse.jetty.server.Connector[] {connector});

    when(threadPool.getBusyThreads()).thenReturn(42);
    when(threadPool.getIdleThreads()).thenReturn(10);
    when(threadPool.getQueueSize()).thenReturn(5);
    when(connector.getConnectedEndPoints()).thenReturn(Collections.nCopies(100, null));

    OtelServerMetrics extension = new OtelServerMetrics();
    extension.install(application, otelTesting.getOpenTelemetry());

    assertGaugeValue("server.jetty.threads.active", 42.0);
    assertGaugeValue("server.jetty.threads.idle", 10.0);
    assertGaugeValue("server.jetty.queue.size", 5.0);
    assertGaugeValue("server.jetty.connections.active", 100.0);
  }

  @Test
  void shouldInstrumentJettyAlternativeImplementations() {
    when(server.getName()).thenReturn("jetty");

    org.eclipse.jetty.server.Server jettyServer = mock(org.eclipse.jetty.server.Server.class);
    org.eclipse.jetty.util.thread.ThreadPool threadPool =
        mock(org.eclipse.jetty.util.thread.ThreadPool.class);
    org.eclipse.jetty.server.Connector genericConnector =
        mock(org.eclipse.jetty.server.Connector.class);

    when(application.require(org.eclipse.jetty.server.Server.class)).thenReturn(jettyServer);
    when(jettyServer.getThreadPool()).thenReturn(threadPool); // Not a QueuedThreadPool
    when(jettyServer.getConnectors())
        .thenReturn(new org.eclipse.jetty.server.Connector[] {genericConnector});

    OtelServerMetrics extension = new OtelServerMetrics();
    extension.install(application, otelTesting.getOpenTelemetry());

    // Fails the ServerConnector instanceof, should be 0
    assertGaugeValue("server.jetty.connections.active", 0.0);

    // Fails the QueuedThreadPool instanceof, metrics shouldn't be registered
    assertThat(otelTesting.getMetrics())
        .noneMatch(m -> m.getName().startsWith("server.jetty.threads"));
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void shouldInstrumentNetty() {
    when(server.getName()).thenReturn("netty");

    NettyEventLoopGroup nettyGroups = mock(NettyEventLoopGroup.class);
    when(application.require(NettyEventLoopGroup.class)).thenReturn(nettyGroups);

    EventLoopGroup eventLoopGroup = mock(EventLoopGroup.class);
    when(nettyGroups.eventLoop()).thenReturn(eventLoopGroup);

    SingleThreadEventExecutor eventLoopExecutor = mock(SingleThreadEventExecutor.class);
    when(eventLoopExecutor.pendingTasks()).thenReturn(15);
    when(eventLoopGroup.iterator())
        .thenAnswer(i -> List.<EventExecutor>of(eventLoopExecutor).iterator());

    EventLoopGroup acceptorGroup = mock(EventLoopGroup.class);
    when(nettyGroups.acceptor()).thenReturn(acceptorGroup);

    SingleThreadEventExecutor acceptorExecutor = mock(SingleThreadEventExecutor.class);
    when(acceptorGroup.iterator())
        .thenAnswer(i -> List.<EventExecutor>of(acceptorExecutor).iterator());

    ThreadPoolExecutor workerPool = mock(ThreadPoolExecutor.class);
    when(workerPool.getActiveCount()).thenReturn(30);

    BlockingQueue queue = mock(BlockingQueue.class);
    when(queue.size()).thenReturn(7);
    when(workerPool.getQueue()).thenReturn(queue);

    when(nettyGroups.worker()).thenReturn(workerPool);

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
    extension.install(application, otelTesting.getOpenTelemetry());

    assertGaugeValue("server.netty.eventloop.pending_tasks", 15.0);
    assertGaugeValue("server.netty.eventloop.count", 1.0);
    assertGaugeValue("server.netty.acceptor.count", 1.0);
    assertGaugeValue("server.netty.worker.threads.active", 30.0);
    assertGaugeValue("server.netty.worker.queue.size", 7.0);
    assertGaugeValue("server.netty.memory.direct_used", 1024.0);
    assertGaugeValue("server.netty.memory.heap_used", 2048.0);
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void shouldInstrumentNettyNativeExecutorsAndVertx() {
    // Also tests the "vertx" routing branch
    when(server.getName()).thenReturn("vertx");

    NettyEventLoopGroup nettyGroups = mock(NettyEventLoopGroup.class);
    when(application.require(NettyEventLoopGroup.class)).thenReturn(nettyGroups);

    EventLoopGroup eventLoopGroup = mock(EventLoopGroup.class);
    when(nettyGroups.eventLoop()).thenReturn(eventLoopGroup);
    // Simulate Acceptor == EventLoop (Disables acceptor metrics)
    when(nettyGroups.acceptor()).thenReturn(eventLoopGroup);

    EventExecutor genericExecutor = mock(EventExecutor.class); // Not a SingleThreadEventExecutor
    when(eventLoopGroup.iterator())
        .thenAnswer(i -> List.<EventExecutor>of(genericExecutor).iterator());

    // Native Netty EventExecutorGroup instead of ThreadPoolExecutor
    io.netty.util.concurrent.EventExecutorGroup nettyWorker =
        mock(io.netty.util.concurrent.EventExecutorGroup.class);
    when(nettyGroups.worker()).thenReturn(nettyWorker);

    SingleThreadEventExecutor workerExecutor = mock(SingleThreadEventExecutor.class);
    when(workerExecutor.pendingTasks()).thenReturn(8);
    // Mixed list to test instanceof branches
    when(nettyWorker.iterator())
        .thenAnswer(i -> List.<EventExecutor>of(workerExecutor, genericExecutor).iterator());

    // Standard Allocator without metrics
    io.netty.buffer.ByteBufAllocator allocator = mock(io.netty.buffer.ByteBufAllocator.class);
    when(application.require(io.netty.buffer.ByteBufAllocator.class)).thenReturn(allocator);

    OtelServerMetrics extension = new OtelServerMetrics();
    extension.install(application, otelTesting.getOpenTelemetry());

    // No pending tasks because genericExecutor is not SingleThreadEventExecutor
    assertGaugeValue("server.netty.eventloop.pending_tasks", 0.0);
    assertGaugeValue("server.netty.eventloop.count", 1.0);

    assertThat(otelTesting.getMetrics())
        .noneMatch(m -> m.getName().equals("server.netty.acceptor.count"));

    assertGaugeValue("server.netty.worker.pending_tasks", 8.0);
    assertGaugeValue("server.netty.worker.threads.count", 2.0);

    assertThat(otelTesting.getMetrics())
        .noneMatch(m -> m.getName().startsWith("server.netty.memory"));
  }

  @Test
  void shouldInstrumentUndertow() {
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

    when(mxBean.getBusyWorkerThreadCount()).thenReturn(64);
    when(mxBean.getWorkerQueueSize()).thenReturn(12);
    when(mxBean.getIoThreadCount()).thenReturn(4);
    when(stats.getActiveConnections()).thenReturn(250L);

    OtelServerMetrics extension = new OtelServerMetrics();
    extension.install(application, otelTesting.getOpenTelemetry());

    assertGaugeValue("server.undertow.worker.threads.active", 64.0);
    assertGaugeValue("server.undertow.worker.queue.size", 12.0);
    assertGaugeValue("server.undertow.eventloop.count", 4.0);
    assertGaugeValue("server.undertow.connections.active", 250.0);
  }

  @Test
  void shouldInstrumentUndertowWithMissingStatistics() {
    when(server.getName()).thenReturn("undertow");

    io.undertow.Undertow undertow = mock(io.undertow.Undertow.class);
    XnioWorker worker = mock(XnioWorker.class);
    XnioWorkerMXBean mxBean = mock(XnioWorkerMXBean.class);
    io.undertow.Undertow.ListenerInfo listenerInfo = mock(io.undertow.Undertow.ListenerInfo.class);

    when(application.require(io.undertow.Undertow.class)).thenReturn(undertow);
    when(undertow.getWorker()).thenReturn(worker);
    when(worker.getMXBean()).thenReturn(mxBean);
    when(undertow.getListenerInfo()).thenReturn(List.of(listenerInfo));
    // Simulate missing statistics (null)
    when(listenerInfo.getConnectorStatistics()).thenReturn(null);

    when(mxBean.getBusyWorkerThreadCount()).thenReturn(30);
    when(mxBean.getWorkerQueueSize()).thenReturn(0);
    when(mxBean.getIoThreadCount()).thenReturn(2);

    OtelServerMetrics extension = new OtelServerMetrics();
    extension.install(application, otelTesting.getOpenTelemetry());

    assertGaugeValue("server.undertow.worker.threads.active", 30.0);
    // Connections should be 0 since stats were null
    assertGaugeValue("server.undertow.connections.active", 0.0);
  }

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
