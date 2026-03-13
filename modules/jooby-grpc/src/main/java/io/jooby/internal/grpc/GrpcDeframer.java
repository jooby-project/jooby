/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.grpc;

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

  /** Processes a chunk of data directly from the server's native ByteBuffer. */
  public void process(ByteBuffer input, Consumer<byte[]> onMessage) {
    while (input.hasRemaining()) {
      if (state == State.HEADER) {
        int toRead = Math.min(headerBuffer.remaining(), input.remaining());

        // Bulk read into header buffer
        int oldLimit = input.limit();
        input.limit(input.position() + toRead);
        headerBuffer.put(input);
        input.limit(oldLimit);

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
        int toRead = Math.min(payloadBuffer.remaining(), input.remaining());

        // Bulk read into payload buffer
        int oldLimit = input.limit();
        input.limit(input.position() + toRead);
        payloadBuffer.put(input);
        input.limit(oldLimit);

        if (!payloadBuffer.hasRemaining()) {
          // The full gRPC message is assembled. Emit it.
          onMessage.accept(payloadBuffer.array());
          headerBuffer.clear();
          payloadBuffer = null;
          state = State.HEADER;
        }
      }
    }
  }
}
