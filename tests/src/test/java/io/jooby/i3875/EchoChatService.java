/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3875;

import io.grpc.stub.StreamObserver;

public class EchoChatService extends ChatServiceGrpc.ChatServiceImplBase {

  @Override
  public StreamObserver<ChatMessage> chatStream(StreamObserver<ChatMessage> responseObserver) {
    return new StreamObserver<ChatMessage>() {
      @Override
      public void onNext(ChatMessage request) {
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
        responseObserver.onCompleted();
      }
    };
  }
}
