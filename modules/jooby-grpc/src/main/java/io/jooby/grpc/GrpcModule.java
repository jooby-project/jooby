/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.grpc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.bridge.SLF4JBridgeHandler;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.grpc.BindableService;
import io.grpc.MethodDescriptor;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.jooby.*;
import io.jooby.internal.grpc.DefaultGrpcProcessor;

public class GrpcModule implements Extension {
  private final List<BindableService> services;
  private final Map<String, MethodDescriptor<?, ?>> registry = new HashMap<>();
  private Server grpcServer;

  static {
    // Optionally remove existing handlers attached to the j.u.l root logger
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    // Install the SLF4J bridge
    SLF4JBridgeHandler.install();
  }

  public GrpcModule(BindableService... services) {
    this.services = List.of(services);
  }

  @Override
  public void install(@NonNull Jooby app) throws Exception {
    var serverName = app.getName();
    var builder = InProcessServerBuilder.forName(serverName);

    // 1. Register user-provided services
    for (var service : services) {
      builder.addService(service);
      for (var method : service.bindService().getMethods()) {
        var descriptor = method.getMethodDescriptor();
        String methodFullName = descriptor.getFullMethodName();
        registry.put(methodFullName, descriptor);
        String routePath = "/" + methodFullName;

        //
        app.post(
            routePath,
            ctx -> {
              throw new IllegalStateException(
                  "gRPC request reached the standard HTTP router for path: "
                      + routePath
                      + ". "
                      + "This means the native gRPC server interceptor was bypassed. "
                      + "Ensure you are running Jetty, Netty, or Undertow with HTTP/2 enabled, "
                      + "and that the GrpcProcessor SPI is correctly loaded.");
            });
      }
    }

    this.grpcServer = builder.build().start();

    // KEEP .directExecutor() here!
    // This ensures that when the background gRPC worker finishes, it instantly pushes
    // the response back to Undertow/Netty without wasting time on another thread hop.
    var channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
    var services = app.getServices();
    var bridge = new DefaultGrpcProcessor(channel, registry);

    // Register it in the Service Registry so the server layer can find it
    services.put(DefaultGrpcProcessor.class, bridge);
    services.put(GrpcProcessor.class, bridge);

    app.onStop(channel::shutdownNow);
    app.onStop(grpcServer::shutdownNow);
  }
}
