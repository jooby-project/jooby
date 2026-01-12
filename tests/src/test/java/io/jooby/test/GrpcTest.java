/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.grpc.GreeterGrpc;
import com.example.grpc.HelloReply;
import com.example.grpc.HelloRequest;
import io.grpc.stub.StreamObserver;
import io.jooby.ServerOptions;
import io.jooby.grpc.GrpcModule;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import okhttp3.*;

public class GrpcTest {

  public class GreeterService extends GreeterGrpc.GreeterImplBase {
    @Override
    public void sayHello(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
      HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + req.getName()).build();
      responseObserver.onNext(reply);
      responseObserver.onCompleted();
    }
  }

  @ServerTest
  public void http2(ServerTestRunner runner) {
    runner
        .options(new ServerOptions().setHttp2(true).setSecurePort(8443))
        .define(
            app -> {
              app.install(new GrpcModule(new GreeterService()));
            })
        .ready(
            (http, https) -> {
              https.get(
                  "/",
                  rsp -> {
                    assertEquals(
                        "{secure=true, protocol=HTTP/2.0, scheme=https}", rsp.body().string());
                  });
              http.get(
                  "/",
                  rsp -> {
                    assertEquals(
                        "{secure=false, protocol=HTTP/1.1, scheme=http}", rsp.body().string());
                  });
            });
  }
}
