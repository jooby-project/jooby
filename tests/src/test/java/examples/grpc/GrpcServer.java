/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples.grpc;

import java.io.IOException;
import java.util.List;

import io.grpc.protobuf.services.ProtoReflectionServiceV1;
import io.jooby.Jooby;
import io.jooby.ServerOptions;
import io.jooby.StartupSummary;
import io.jooby.grpc.GrpcModule;
import io.jooby.handler.AccessLogHandler;
import io.jooby.jetty.JettyServer;

public class GrpcServer extends Jooby {

  {
    setStartupSummary(List.of(StartupSummary.VERBOSE));
    use(new AccessLogHandler());
    install(
        new GrpcModule(
            new GreeterService(), new ChatServiceImpl(), ProtoReflectionServiceV1.newInstance()));
  }

  public static void main(final String[] args) throws InterruptedException, IOException {
    runApp(
        args,
        new JettyServer(new ServerOptions().setSecurePort(8443).setHttp2(true)),
        GrpcServer::new);

    // Build the server
    //    Server server = io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder.forPort(9090)
    //        .addService(new GreeterService())
    //        .addService(ProtoReflectionServiceV1.newInstance())// Your generated service
    // implementation
    //        .build();
    //
    //    // Start the server
    //    server.start();
    //    System.out.println("Server started on port 9090");
    //
    //    // Keep the main thread alive until the server is shut down
    //    server.awaitTermination();
  }
}
