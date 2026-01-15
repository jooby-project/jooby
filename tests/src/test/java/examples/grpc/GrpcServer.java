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
import io.jooby.undertow.UndertowServer;

public class GrpcServer extends Jooby {

  {
    setStartupSummary(List.of(StartupSummary.VERBOSE));
    use(new AccessLogHandler());
    install(
        new GrpcModule(
            new GreeterService(), new ChatServiceImpl(), ProtoReflectionServiceV1.newInstance()));
  }

  //  INFO  [2026-01-15 10:19:29,307] [worker-55] UnifiedGrpcBridge method type: BIDI_STREAMING
  //  INFO  [2026-01-15 10:19:29,308] [worker-55] JettySubscription init request(1)
  //  INFO  [2026-01-15 10:19:29,308] [worker-55] JettySubscription 1- start reading request
  //  INFO  [2026-01-15 10:19:29,308] [worker-55] JettySubscription 1- byte read: 00000000033a012a
  //  INFO  [2026-01-15 10:19:29,308] [worker-55] GrpcRequestBridge deframe 3a012a
  //  INFO  [2026-01-15 10:19:29,308] [worker-55] UnifiedGrpcBridge onNext Send
  // 12033a012a32460a120a10746573742e43686174536572766963650a250a23677270632e7265666c656374696f6e2e76312e5365727665725265666c656374696f6e0a090a0747726565746572
  //  INFO  [2026-01-15 10:19:29,308] [worker-55] GrpcRequestBridge asking for more request(1)
  //  INFO  [2026-01-15 10:19:29,308] [worker-55] JettySubscription 1- demanding more
  //  INFO  [2026-01-15 10:19:29,308] [worker-55] JettySubscription 1- finish reading request
  //  INFO  [2026-01-15 10:19:29,309] [worker-52] JettySubscription 1.demand- start reading request
  //  INFO  [2026-01-15 10:19:29,309] [worker-52] JettySubscription 1.demand- last reach
  //  INFO  [2026-01-15 10:19:29,309] [worker-52] JettySubscription handle complete
  //  INFO  [2026-01-15 10:19:29,309] [worker-52] UnifiedGrpcBridge onCompleted
  //  INFO  [2026-01-15 10:19:29,309] [worker-52] JettySubscription 1.demand- finish reading request
  //  INFO  [2026-01-15 10:20:08,267] [Thread-0] GrpcServer Stopped GrpcServer

  public static void main(final String[] args) throws InterruptedException, IOException {
    runApp(
        args,
        new UndertowServer(new ServerOptions().setSecurePort(8443).setHttp2(true)),
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
