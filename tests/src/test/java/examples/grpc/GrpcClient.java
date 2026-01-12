/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples.grpc;

import com.example.grpc.GreeterGrpc;
import com.example.grpc.HelloReply;
import com.example.grpc.HelloRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class GrpcClient {
  public static void main(String[] args) {
    ManagedChannel channel =
        ManagedChannelBuilder.forAddress("localhost", 8080).usePlaintext().build();

    GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(channel);

    HelloReply response = stub.sayHello(HelloRequest.newBuilder().setName("Edgar").build());
    System.out.println(response.getMessage());

    channel.shutdown();
  }
}
