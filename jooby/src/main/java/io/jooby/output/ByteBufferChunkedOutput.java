/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.output;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.SneakyThrows;

class ByteBufferChunkedOutput implements ChunkedOutput {
  private final List<ByteBuffer> chunks = new ArrayList<>();
  private int size = 0;
  private int bufferSizeHint = BUFFER_SIZE;

  @Override
  public List<ByteBuffer> getChunks() {
    return chunks;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public Output write(byte b) {
    addChunk(ByteBuffer.wrap(new byte[] {b}));
    return this;
  }

  @Override
  public Output write(byte[] source) {
    addChunk(ByteBuffer.wrap(source));
    return this;
  }

  @Override
  public Output write(byte[] source, int offset, int length) {
    addChunk(ByteBuffer.wrap(source, offset, length));
    return this;
  }

  @Override
  public void close() throws IOException {
    // NOOP
  }

  /**
   * Expensive operation.
   *
   * @return A byte buffer.
   */
  @Override
  public ByteBuffer asByteBuffer() {
    var buf = ByteBuffer.allocate(size);
    chunks.forEach(buf::put);
    buf.flip();
    return buf;
  }

  @Override
  public String asString(@NonNull Charset charset) {
    var sb = new StringBuilder();
    chunks.forEach(bytes -> sb.append(charset.decode(bytes)));
    return sb.toString();
  }

  @Override
  public void accept(SneakyThrows.Consumer<ByteBuffer> consumer) {
    chunks.forEach(consumer);
  }

  @Override
  public int bufferSizeHint() {
    return bufferSizeHint;
  }

  @Override
  public String toString() {
    return asString(StandardCharsets.UTF_8);
  }

  @Override
  public void send(Context ctx) {
    ctx.send(chunks.toArray(new ByteBuffer[0]));
  }

  private void addChunk(ByteBuffer chunk) {
    chunks.add(chunk);
    int length = chunk.remaining();
    size += length;
    if (bufferSizeHint < length) {
      bufferSizeHint = length;
    }
  }
}
