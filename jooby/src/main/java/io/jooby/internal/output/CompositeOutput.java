/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.output;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.SneakyThrows;
import io.jooby.output.BufferedOutput;

/**
 * Merge buffers into one.
 *
 * @author edgar
 * @since 4.0.0
 */
public class CompositeOutput implements BufferedOutput {
  private final List<ByteBuffer> chunks = new ArrayList<>();
  private int size = 0;

  @Override
  public int size() {
    return size;
  }

  @Override
  public BufferedOutput write(byte b) {
    addChunk(ByteBuffer.wrap(new byte[] {b}));
    return this;
  }

  @Override
  public BufferedOutput write(byte[] source) {
    addChunk(ByteBuffer.wrap(source));
    return this;
  }

  @Override
  public BufferedOutput write(byte[] source, int offset, int length) {
    addChunk(ByteBuffer.wrap(source, offset, length));
    return this;
  }

  @Override
  public BufferedOutput clear() {
    chunks.forEach(ByteBuffer::clear);
    chunks.clear();
    return this;
  }

  /**
   * Expensive operation.
   *
   * @return A byte buffer.
   */
  @Override
  public ByteBuffer asByteBuffer() {
    var buf = ByteBuffer.allocate(size);
    for (ByteBuffer chunk : chunks) {
      buf.put(chunk.duplicate());
    }
    buf.flip();
    return buf;
  }

  @Override
  public void transferTo(@NonNull SneakyThrows.Consumer<ByteBuffer> consumer) {
    chunks.forEach(consumer);
  }

  @Override
  public String toString() {
    return "chunks=" + chunks.size() + ", size=" + size;
  }

  @Override
  public void send(Context ctx) {
    ctx.send(chunks.toArray(new ByteBuffer[0]));
  }

  private void addChunk(ByteBuffer chunk) {
    chunks.add(chunk);
    size += chunk.remaining();
  }
}
