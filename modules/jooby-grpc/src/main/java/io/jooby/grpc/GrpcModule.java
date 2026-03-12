/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.grpc;

import java.util.List;

import org.slf4j.bridge.SLF4JBridgeHandler;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionServiceV1;
import io.jooby.*;

public class GrpcModule implements Extension {
  private final List<BindableService> services;
  private final GrpcMethodRegistry methodRegistry = new GrpcMethodRegistry();
  private final String serverName = "jooby-internal-" + System.nanoTime();
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
    var builder = InProcessServerBuilder.forName(serverName).directExecutor();

    // 1. Register user-provided services
    for (BindableService service : services) {
      builder.addService(service);
      methodRegistry.registerService(service);
    }

    BindableService reflectionService = ProtoReflectionServiceV1.newInstance();
    builder.addService(reflectionService);
    methodRegistry.registerService(reflectionService);

    this.grpcServer = builder.build().start();

    var channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();

    var bridge = new UnifiedGrpcBridge(channel, methodRegistry);
    // Register it in the Service Registry so the server layer can find it
    app.getServices().put(UnifiedGrpcBridge.class, bridge);

    app.getServices().put(GrpcProcessor.class, bridge);

    // Mount the bridge.
    // app.post("/*", bridge);

    app.onStop(
        () -> {
          channel.shutdownNow();
          grpcServer.shutdownNow();
        });
  }
}
