/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.output;

import java.io.Closeable;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.IntPredicate;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.SneakyThrows;

public interface Output extends Closeable {
  /** Default buffer size: <code>4k</code>. */
  int BUFFER_SIZE = 4096;

  default OutputStream asOutputStream() {
    return new OutputOutputStream(this);
  }

  default Writer asWriter() {
    return asWriter(StandardCharsets.UTF_8);
  }

  default Writer asWriter(@NonNull Charset charset) {
    return new OutputWriter(this, charset);
  }

  ByteBuffer asByteBuffer();

  String asString(@NonNull Charset charset);

  default void toByteBuffer(ByteBuffer dest) {
    toByteBuffer(0, dest, dest.position(), size());
  }

  default Iterator<ByteBuffer> split(IntPredicate predicate) {
    // TODO: fix me for chunks
    var buffer = asByteBuffer();
    var chunks = new ArrayList<ByteBuffer>();
    var offset = 0;
    for (int i = 0; i < buffer.remaining(); i++) {
      var b = buffer.get(i);
      if (predicate.test(b)) {
        chunks.add(buffer.duplicate().position(offset).limit(i + 1));
        offset = i + 1;
      }
    }
    if (offset < buffer.remaining()) {
      chunks.add(buffer.duplicate().position(offset));
    }
    return chunks.iterator();
  }

  /**
   * Copies the given length from this data buffer into the given destination {@code ByteBuffer},
   * beginning at the given source position, and the given destination position in the destination
   * byte buffer.
   *
   * @param srcPos the position of this data buffer from where copying should start
   * @param dest the destination byte buffer
   * @param destPos the position in {@code dest} to where copying should start
   * @param length the amount of data to copy
   */
  default void toByteBuffer(int srcPos, ByteBuffer dest, int destPos, int length) {
    dest = dest.duplicate().clear();
    dest.put(destPos, asByteBuffer(), srcPos, length);
  }

  void accept(SneakyThrows.Consumer<ByteBuffer> consumer);

  default Iterator<ByteBuffer> iterator() {
    var list = new ArrayList<ByteBuffer>();
    accept(list::add);
    return list.iterator();
  }

  /**
   * Total size in number of bytes of the output.
   *
   * @return size
   */
  int size();

  /**
   * The recommend buffer size to use for extracting with
   *
   * @return buffer size to use which by default is {@link #size()}.
   */
  default int bufferSizeHint() {
    return size();
  }

  /**
   * Write a single byte into this buffer at the current writing position.
   *
   * @param b the byte to be written
   * @return this output
   */
  Output write(byte b);

  /**
   * Write the given source into this buffer, starting at the current writing position of this
   * buffer.
   *
   * @param source the bytes to be written into this buffer
   * @return this output
   */
  Output write(byte[] source);

  /**
   * Write at most {@code length} bytes of the given source into this buffer, starting at the
   * current writing position of this buffer.
   *
   * @param source the bytes to be written into this buffer
   * @param offset the index within {@code source} to start writing from
   * @param length the maximum number of bytes to be written from {@code source}
   * @return this output
   */
  Output write(byte[] source, int offset, int length);

  /**
   * Write the given {@code String} using {@code UTF-8}, starting at the current writing position.
   *
   * @param source the char sequence to write into this buffer
   * @return this output
   */
  default Output write(@NonNull String source) {
    return write(source, StandardCharsets.UTF_8);
  }

  /**
   * Write the given {@code String} using the given {@code Charset}, starting at the current writing
   * position.
   *
   * @param source the char sequence to write into this buffer
   * @param charset the charset to encode the char sequence with
   * @return this output
   */
  default Output write(@NonNull String source, @NonNull Charset charset) {
    if (!source.isEmpty()) {
      return write(source.getBytes(charset));
    }
    return this;
  }

  /**
   * Write the given source into this buffer, starting at the current writing position of this
   * buffer.
   *
   * @param source the bytes to be written into this buffer
   * @return this output
   */
  default Output write(@NonNull ByteBuffer source) {
    if (source.hasArray()) {
      return write(source.array(), source.arrayOffset() + source.position(), source.remaining());
    } else {
      var bytes = new byte[source.remaining()];
      source.get(bytes);
      return write(bytes);
    }
  }

  default Output write(@NonNull CharBuffer source, @NonNull Charset charset) {
    if (!source.isEmpty()) {
      return write(charset.encode(source));
    }
    return this;
  }

  static Output wrap(ByteBuffer buffer) {
    return new WrappedOutput(buffer);
  }

  static Output wrap(byte[] bytes) {
    return new WrappedOutput(bytes);
  }

  static Output wrap(byte[] bytes, int offset, int length) {
    return new WrappedOutput(bytes, offset, length);
  }

  static Output wrap(String value) {
    return wrap(value, StandardCharsets.UTF_8);
  }

  static Output wrap(String value, Charset charset) {
    return wrap(value.getBytes(charset));
  }

  void send(io.jooby.Context ctx);
}
