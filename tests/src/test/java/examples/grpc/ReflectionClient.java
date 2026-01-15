/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples.grpc;

import java.util.concurrent.CountDownLatch;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.reflection.v1.ServerReflectionGrpc;
import io.grpc.reflection.v1.ServerReflectionRequest;
import io.grpc.reflection.v1.ServerReflectionResponse;
import io.grpc.stub.StreamObserver;

public class ReflectionClient {
  public static void main(String[] args) throws InterruptedException {
    ManagedChannel channel =
        ManagedChannelBuilder.forAddress("localhost", 8080).usePlaintext().build();
    try {
      var latch = new CountDownLatch(1);
      ServerReflectionGrpc.ServerReflectionStub stub = ServerReflectionGrpc.newStub(channel);

      // 1. Prepare the response observer
      StreamObserver<ServerReflectionResponse> responseObserver =
          new StreamObserver<>() {
            @Override
            public void onNext(ServerReflectionResponse response) {
              // This is the part that returns the list of services
              response
                  .getListServicesResponse()
                  .getServiceList()
                  .forEach(
                      s -> {
                        System.out.println("Service: " + s.getName());
                      });
            }

            @Override
            public void onError(Throwable t) {
              t.printStackTrace();
            }

            @Override
            public void onCompleted() {
              latch.countDown();
            }
          };

      // 2. Open the bidirectional stream
      StreamObserver<ServerReflectionRequest> requestObserver =
          stub.serverReflectionInfo(responseObserver);

      // 3. Send the "List Services" request
      requestObserver.onNext(
          ServerReflectionRequest.newBuilder()
              .setListServices("") // The trigger for 'list'
              .setHost("localhost")
              .build());

      // 4. Signal half-close (Very important for reflection)
      requestObserver.onCompleted();

      latch.await();
    } finally {
      channel.shutdown();
    }
  }
}
