/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples.grpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.grpc.ChatMessage;
import com.example.grpc.ChatServiceGrpc;
import io.grpc.stub.StreamObserver;

public class ChatServiceImpl extends ChatServiceGrpc.ChatServiceImplBase {
  private final Logger log = LoggerFactory.getLogger(getClass());

  @Override
  public StreamObserver<ChatMessage> chatStream(StreamObserver<ChatMessage> responseObserver) {
    return new StreamObserver<ChatMessage>() {
      @Override
      public void onNext(ChatMessage request) {
        log.info("Got message: {}", request.getTextBytes());
        // Logic: Echo back the text with a prefix
        ChatMessage response =
            ChatMessage.newBuilder()
                .setUser("Server")
                .setText("Echo: " + request.getText())
                .build();

        responseObserver.onNext(response);
      }

      @Override
      public void onError(Throwable t) {
        System.err.println("Stream error: " + t.getMessage());
      }

      @Override
      public void onCompleted() {
        log.info("Chat closed");
        responseObserver.onCompleted();
      }
    };
  }
}
