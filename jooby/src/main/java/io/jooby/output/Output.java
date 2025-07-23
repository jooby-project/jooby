/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.output;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.SneakyThrows;

/**
 * Output used to support multiple implementations like byte array, byte buffer, netty buffers.
 *
 * @author edgar
 * @since 4.0.0
 * @see BufferedOutput
 */
public interface Output {

  /**
   * A read-only view as {@link ByteBuffer}.
   *
   * @return A read-only byte buffer.
   */
  ByteBuffer asByteBuffer();

  /**
   * Transfers the entire buffered output (one or multiple buffers) to a consumer. {@link
   * ByteBuffer} are read-only.
   *
   * @param consumer Consumer.
   */
  void transferTo(@NonNull SneakyThrows.Consumer<ByteBuffer> consumer);

  /**
   * An iterator over read-only byte buffers.
   *
   * @return An iterator over read-only byte buffers.
   */
  default Iterator<ByteBuffer> iterator() {
    var list = new ArrayList<ByteBuffer>();
    transferTo(list::add);
    return list.iterator();
  }

  /**
   * Total size in number of bytes of the output.
   *
   * @return Total size in number of bytes of the output.
   */
  int size();

  /**
   * Send the output to the client.
   *
   * @param ctx Context.
   */
  void send(io.jooby.Context ctx);
}
