/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3875;

import io.grpc.stub.StreamObserver;

public class EchoGreeterService extends GreeterGrpc.GreeterImplBase {

  private final EchoService echoService;

  @jakarta.inject.Inject
  public EchoGreeterService(EchoService echoService) {
    this.echoService = echoService;
  }

  @Override
  public void sayHello(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
    HelloReply reply = HelloReply.newBuilder().setMessage(echoService.echo(req.getName())).build();
    responseObserver.onNext(reply);
    responseObserver.onCompleted();
  }
}
