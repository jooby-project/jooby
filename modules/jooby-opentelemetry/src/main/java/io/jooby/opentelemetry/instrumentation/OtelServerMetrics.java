/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.opentelemetry.instrumentation;

import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import io.jooby.Jooby;
import io.jooby.Server;
import io.jooby.netty.NettyEventLoopGroup;
import io.jooby.opentelemetry.OtelExtension;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;

/**
 * OpenTelemetry extension for Jooby HTTP servers.
 *
 * <p>This extension automatically detects the underlying HTTP server running your Jooby application
 * (Jetty, Netty, or Undertow) and exports native, server-specific operational metrics to your
 * OpenTelemetry backend under the {@code io.jooby.server} meter.
 *
 * <h3>Supported Servers & Metrics</h3>
 *
 * <h4>Netty/Vert.x</h4>
 *
 * <ul>
 *   <li>{@code server.netty.eventloop.pending_tasks} / {@code count}: Tracks IO event loop threads
 *       and pending tasks. High pending tasks often indicate blocking code on the event loop.
 *   <li>{@code server.netty.acceptor.count}: Tracks dedicated TCP acceptor threads.
 *   <li>{@code server.netty.worker.*}: Tracks active threads, queue sizes, and pending tasks in the
 *       worker executor.
 *   <li>{@code server.netty.memory.direct_used} / {@code heap_used}: Tracks ByteBufAllocator memory
 *       consumption.
 * </ul>
 *
 * <h4>Jetty</h4>
 *
 * <ul>
 *   <li>{@code server.jetty.threads.active} / {@code idle}: Tracks the state of the underlying
 *       {@link QueuedThreadPool}.
 *   <li>{@code server.jetty.queue.size}: Tracks jobs queued waiting for an available Jetty thread.
 *   <li>{@code server.jetty.connections.active}: Tracks active TCP connections across all server
 *       connectors.
 * </ul>
 *
 * <h4>Undertow</h4>
 *
 * <ul>
 *   <li>{@code server.undertow.worker.threads.active} / {@code queue.size}: Tracks the XNIO worker
 *       pool capacity and backlog.
 *   <li>{@code server.undertow.eventloop.count}: Tracks active IO (Event Loop) threads managed by
 *       XNIO.
 *   <li>{@code server.undertow.connections.active}: Tracks active connections across all Undertow
 *       listeners.
 * </ul>
 *
 * <h3>Usage</h3>
 *
 * <p>Register this extension inside the core {@code OtelModule} during application setup:
 *
 * <pre>{@code
 * {
 * install(new OtelModule(
 * new OtelServerMetrics()
 * ));
 * }
 * }</pre>
 *
 * @since 4.3.1
 * @author edgar
 */
public class OtelServerMetrics implements OtelExtension {

  @Override
  public void install(Jooby application, OpenTelemetry openTelemetry) {

    var server = application.require(Server.class);
    var meter = openTelemetry.getMeter("io.jooby.server");

    // Route the instrumentation based on the active server
    switch (server.getName().toLowerCase()) {
      case "jetty":
        instrumentJetty(application, meter);
        break;
      case "netty", "vertx":
        instrumentNetty(application, meter);
        break;
      case "undertow":
        instrumentUndertow(application, meter);
        break;
      default:
        application
            .getLog()
            .debug("No specific OTel metrics mapped for server: {}", server.getName());
    }
  }

  private void instrumentJetty(Jooby application, Meter meter) {
    var jettyServer = application.require(org.eclipse.jetty.server.Server.class);

    if (jettyServer.getThreadPool() instanceof QueuedThreadPool threadPool) {
      meter
          .gaugeBuilder("server.jetty.threads.active")
          .setDescription("Number of active (busy) threads in Jetty pool")
          .setUnit("{thread}")
          .buildWithCallback(m -> m.record(threadPool.getBusyThreads()));

      meter
          .gaugeBuilder("server.jetty.threads.idle")
          .setDescription("Number of idle threads in Jetty pool")
          .setUnit("{thread}")
          .buildWithCallback(m -> m.record(threadPool.getIdleThreads()));

      meter
          .gaugeBuilder("server.jetty.queue.size")
          .setDescription("Number of jobs queued waiting for a Jetty thread")
          .setUnit("{job}")
          .buildWithCallback(m -> m.record(threadPool.getQueueSize()));
    }

    meter
        .gaugeBuilder("server.jetty.connections.active")
        .setDescription("Number of active TCP connections to Jetty")
        .setUnit("{connection}")
        .buildWithCallback(
            m -> {
              long totalConnections = 0;
              for (var connector : jettyServer.getConnectors()) {
                if (connector instanceof ServerConnector serverConnector) {
                  totalConnections += serverConnector.getConnectedEndPoints().size();
                }
              }
              m.record(totalConnections);
            });
  }

  private void instrumentNetty(Jooby application, Meter meter) {
    var nettyGroups = application.require(NettyEventLoopGroup.class);
    // --- 1. EVENT LOOP (IO / CHILD) METRICS ---
    meter
        .gaugeBuilder("server.netty.eventloop.pending_tasks")
        .setDescription(
            "Number of pending tasks in Netty IO event loops. High numbers indicate blocking code.")
        .setUnit("{task}")
        .buildWithCallback(
            m -> {
              long totalPending = 0;
              for (var eventExecutor : nettyGroups.eventLoop()) {
                if (eventExecutor
                    instanceof io.netty.util.concurrent.SingleThreadEventExecutor stee) {
                  totalPending += stee.pendingTasks();
                }
              }
              m.record(totalPending);
            });

    meter
        .gaugeBuilder("server.netty.eventloop.count")
        .setDescription("Number of active Netty IO event loop threads")
        .setUnit("{thread}")
        .buildWithCallback(
            m -> {
              long count = 0;
              for (var ignored : nettyGroups.eventLoop()) {
                count++;
              }
              m.record(count);
            });

    // --- 2. ACCEPTOR METRICS ---
    // Safely verify the acceptor exists AND is a distinct pool from the EventLoop
    if (nettyGroups.acceptor() != nettyGroups.eventLoop()) {
      meter
          .gaugeBuilder("server.netty.acceptor.count")
          .setDescription("Number of active acceptor threads handling TCP connections")
          .setUnit("{thread}")
          .buildWithCallback(
              m -> {
                long count = 0;
                for (var ignored : nettyGroups.acceptor()) count++;
                m.record(count);
              });
    }

    // --- 3. WORKER EXECUTOR METRICS ---
    var worker = nettyGroups.worker();

    if (worker instanceof java.util.concurrent.ThreadPoolExecutor threadPool) {
      meter
          .gaugeBuilder("server.netty.worker.threads.active")
          .setDescription("Number of active threads in the Java worker pool")
          .setUnit("{thread}")
          .buildWithCallback(m -> m.record(threadPool.getActiveCount()));

      meter
          .gaugeBuilder("server.netty.worker.queue.size")
          .setDescription("Number of tasks queued waiting for a Java worker thread")
          .setUnit("{task}")
          .buildWithCallback(m -> m.record(threadPool.getQueue().size()));

      // Scenario B: Worker is a native Netty DefaultEventExecutorGroup
    } else if (worker instanceof io.netty.util.concurrent.EventExecutorGroup nettyExecutor) {
      meter
          .gaugeBuilder("server.netty.worker.pending_tasks")
          .setDescription("Number of pending tasks in the Netty EventExecutorGroup")
          .setUnit("{task}")
          .buildWithCallback(
              m -> {
                long totalPending = 0;
                for (var executor : nettyExecutor) {
                  if (executor instanceof io.netty.util.concurrent.SingleThreadEventExecutor stee) {
                    totalPending += stee.pendingTasks();
                  }
                }
                m.record(totalPending);
              });

      meter
          .gaugeBuilder("server.netty.worker.threads.count")
          .setDescription("Number of active Netty worker threads")
          .setUnit("{thread}")
          .buildWithCallback(
              m -> {
                long count = 0;
                for (var ignored : nettyExecutor) count++;
                m.record(count);
              });
    }

    // --- 4. GLOBAL MEMORY METRICS ---
    var allocator = application.require(io.netty.buffer.ByteBufAllocator.class);

    if (allocator instanceof io.netty.buffer.ByteBufAllocatorMetricProvider metricProvider) {
      var metric = metricProvider.metric();
      meter
          .gaugeBuilder("server.netty.memory.direct_used")
          .setDescription("Used direct memory by Netty ByteBufAllocator")
          .setUnit("By")
          .buildWithCallback(m -> m.record(metric.usedDirectMemory()));

      meter
          .gaugeBuilder("server.netty.memory.heap_used")
          .setDescription("Used heap memory by Netty ByteBufAllocator")
          .setUnit("By")
          .buildWithCallback(m -> m.record(metric.usedHeapMemory()));
    }
  }

  private void instrumentUndertow(Jooby application, Meter meter) {
    var undertow = application.require(io.undertow.Undertow.class);
    var worker = undertow.getWorker();

    // Extract the public management bean to read the thread states safely
    var mxBean = worker.getMXBean();

    // 1. Worker Pool Metrics
    meter
        .gaugeBuilder("server.undertow.worker.threads.active")
        .setDescription("Number of active task threads in the XNIO worker pool")
        .setUnit("{thread}")
        .buildWithCallback(m -> m.record(mxBean.getBusyWorkerThreadCount()));

    meter
        .gaugeBuilder("server.undertow.worker.queue.size")
        .setDescription("Number of tasks queued in the XNIO worker")
        .setUnit("{task}")
        .buildWithCallback(m -> m.record(mxBean.getWorkerQueueSize()));

    // 2. Event Loop (IO Thread) Count
    meter
        .gaugeBuilder("server.undertow.eventloop.count")
        .setDescription("Number of active IO (Event Loop) threads managed by XNIO")
        .setUnit("{thread}")
        .buildWithCallback(m -> m.record(mxBean.getIoThreadCount()));

    // 3. Event Loop Load (Via Connector Statistics)
    meter
        .gaugeBuilder("server.undertow.connections.active")
        .setDescription("Active connections being managed by the Undertow event loops")
        .setUnit("{connection}")
        .buildWithCallback(
            m -> {
              long activeConnections = 0;
              for (var listener : undertow.getListenerInfo()) {
                var stats = listener.getConnectorStatistics();
                if (stats != null) {
                  activeConnections += stats.getActiveConnections();
                }
              }
              m.record(activeConnections);
            });
  }
}
