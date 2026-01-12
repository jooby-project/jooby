/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.grpc;

import java.util.List;
import java.util.function.Function;

import io.grpc.*;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.jooby.*;

public class GrpcModule implements Extension {
  private final List<BindableService> services;
  private final GrpcMethodRegistry methodRegistry = new GrpcMethodRegistry();
  private final String serverName = "jooby-internal-" + System.nanoTime();
  private Server grpcServer;

  public GrpcModule(BindableService... services) {
    this.services = List.of(services);
  }

  @Override
  public void install(Jooby app) throws Exception {
    // 1. Start an In-Process gRPC Server (Memory only)
    var builder = InProcessServerBuilder.forName(serverName).directExecutor();
    for (BindableService service : services) {
      builder.addService(service);
      methodRegistry.registerService(service);
    }

    this.grpcServer = builder.build().start();

    // 2. Create the Channel to talk to it
    var channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();

    var handler = new UnifiedGrpcBridge(channel, methodRegistry);
    app.getServices().put(ServiceKey.key(Function.class, "gRPC"), handler);
    // 3. Register the bridge route
    // gRPC paths are always /{package.Service}/{Method}
    // app.post("/{service}/{method}", ReactiveSupport.concurrent(new GrpcHandler(channel,
    // methodRegistry)));

    app.onStop(
        () -> {
          channel.shutdownNow();
          grpcServer.shutdownNow();
        });
  }
}
