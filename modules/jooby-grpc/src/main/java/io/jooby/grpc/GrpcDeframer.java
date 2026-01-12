/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.grpc;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public class GrpcDeframer {
  private enum State {
    HEADER,
    PAYLOAD
  }

  private State state = State.HEADER;
  private final ByteBuffer headerBuffer = ByteBuffer.allocate(5);
  private ByteBuffer payloadBuffer;

  public void process(byte[] data, Consumer<byte[]> onMessage) {
    ByteBuffer input = ByteBuffer.wrap(data);
    while (input.hasRemaining()) {
      if (state == State.HEADER) {
        while (headerBuffer.hasRemaining() && input.hasRemaining()) {
          headerBuffer.put(input.get());
        }
        if (!headerBuffer.hasRemaining()) {
          headerBuffer.flip();
          headerBuffer.get(); // skip compressed flag
          int length = headerBuffer.getInt();
          if (length == 0) {
            onMessage.accept(new byte[0]);
            headerBuffer.clear();
          } else {
            payloadBuffer = ByteBuffer.allocate(length);
            state = State.PAYLOAD;
          }
        }
      } else if (state == State.PAYLOAD) {
        while (payloadBuffer.hasRemaining() && input.hasRemaining()) {
          payloadBuffer.put(input.get());
        }
        if (!payloadBuffer.hasRemaining()) {
          onMessage.accept(payloadBuffer.array());
          headerBuffer.clear();
          payloadBuffer = null;
          state = State.HEADER;
        }
      }
    }
  }
}
