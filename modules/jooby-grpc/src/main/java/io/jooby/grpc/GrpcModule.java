/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.grpc;

import java.util.List;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionServiceV1;
import io.jooby.Extension;
import io.jooby.Jooby;

public class GrpcModule implements Extension {
  private final List<BindableService> services;
  private final GrpcMethodRegistry methodRegistry = new GrpcMethodRegistry();
  private final String serverName = "jooby-internal-" + System.nanoTime();
  private Server grpcServer;

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

    // 2. Register stable gRPC Server Reflection (v1)
    BindableService reflectionService = ProtoReflectionServiceV1.newInstance();
    builder.addService(reflectionService);
    methodRegistry.registerService(reflectionService);

    this.grpcServer = builder.build().start();

    var channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();

    UnifiedGrpcBridge bridge = new UnifiedGrpcBridge(channel, methodRegistry);

    // Mount the bridge.
    app.post("/*", bridge);

    app.onStop(
        () -> {
          channel.shutdownNow();
          grpcServer.shutdownNow();
        });
  }
}
