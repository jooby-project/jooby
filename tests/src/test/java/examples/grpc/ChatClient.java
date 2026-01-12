/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples.grpc;

import java.util.concurrent.CountDownLatch;

import com.example.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

public class ChatClient {
  public static void main(String[] args) throws InterruptedException {
    // 1. Create a channel to your JOOBY server
    ManagedChannel channel =
        ManagedChannelBuilder.forAddress("localhost", 8080)
            .usePlaintext() // Assuming the bridge is HTTP/2 Cleartext
            .build();

    // 2. Create an ASYNC stub (BiDi requires the async stub)
    ChatServiceGrpc.ChatServiceStub asyncStub = ChatServiceGrpc.newStub(channel);

    // This latch helps the main thread wait until the stream is fully finished
    CountDownLatch latch = new CountDownLatch(3);

    // 3. Define the observer to handle responses coming BACK from the Bridge
    StreamObserver<ChatMessage> responseObserver =
        new StreamObserver<>() {
          @Override
          public void onNext(ChatMessage value) {
            System.out.println(
                "Received from Bridge: [" + value.getUser() + "] " + value.getText());
            latch.countDown();
          }

          @Override
          public void onError(Throwable t) {
            System.err.println("Bridge Error: " + t.getMessage());
            t.printStackTrace();
            latch.countDown();
            latch.countDown();
            latch.countDown();
          }

          @Override
          public void onCompleted() {
            System.out.println("Bridge closed the stream (Trailers received successfully).");
            latch.countDown();
          }
        };

    // 4. Start the call. Returns the observer we use to SEND messages TO the Bridge.
    StreamObserver<ChatMessage> requestObserver = asyncStub.chatStream(responseObserver);

    try {
      System.out.println("Connecting to Bridge and sending messages...");

      // 5. Send a stream of messages over time
      requestObserver.onNext(
          ChatMessage.newBuilder().setUser("JavaClient").setText("Ping 1").build());

      Thread.sleep(1000); // Simulate network/processing delay

      requestObserver.onNext(
          ChatMessage.newBuilder().setUser("JavaClient").setText("Ping 2").build());

      // 6. Tell the Bridge we are done sending data
      requestObserver.onCompleted();

    } catch (Exception e) {
      requestObserver.onError(e);
    }
    latch.await();

    // Wait for the server to finish responding (timeout after 10 seconds)
    channel.shutdown();
  }
}
