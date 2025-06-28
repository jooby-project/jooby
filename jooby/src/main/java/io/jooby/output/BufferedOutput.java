/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.output;

import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.SneakyThrows;
import io.jooby.internal.output.OutputOutputStream;
import io.jooby.internal.output.OutputWriter;

/**
 * Buffered output used to support multiple implementations like byte array, byte buffer, netty
 * buffers.
 *
 * <p>There are two implementations of output one is backed by a {@link ByteBuffer} and the other is
 * a view of multiple {@link ByteBuffer} byffers. See {@link
 * BufferedOutputFactory#newBufferedOutput()} and {@link BufferedOutputFactory#newCompositeOutput()}
 *
 * @author edgar
 * @since 4.0.0
 */
public interface BufferedOutput {
  /**
   * This output as an output stream. Changes made to the output stream are reflected in this
   * output.
   *
   * @return An output stream.
   */
  default OutputStream asOutputStream() {
    return new OutputOutputStream(this);
  }

  /**
   * This output as a writer. Changes made to the writer are reflected in this output. Bytes are
   * written using the {@link StandardCharsets#UTF_8} charset.
   *
   * @return An output stream.
   */
  default Writer asWriter() {
    return asWriter(StandardCharsets.UTF_8);
  }

  /**
   * This output as a writer. Changes made to the writer are reflected in this output.
   *
   * @param charset Charset to use.
   * @return An output stream.
   */
  default Writer asWriter(@NonNull Charset charset) {
    return new OutputWriter(this, charset);
  }

  /**
   * A view of internal bytes as {@link byte buffer} changes made to the buffer are reflected in
   * this output.
   *
   * @return A byte byffer.
   */
  ByteBuffer asByteBuffer();

  /**
   * A view of internal bytes as string.
   *
   * @param charset Charset to use.
   * @return A string.
   */
  String asString(@NonNull Charset charset);

  /**
   * Transfers the entire buffered output (one or multiple buffers) to a consumer.
   *
   * @param consumer Consumer.
   */
  void transferTo(@NonNull SneakyThrows.Consumer<ByteBuffer> consumer);

  /**
   * An iterator over byte buffers.
   *
   * @return An iterator over byte buffers.
   */
  default Iterator<ByteBuffer> iterator() {
    var list = new ArrayList<ByteBuffer>();
    transferTo(list::add);
    return list.iterator();
  }

  /**
   * Total size in number of bytes of the output.
   *
   * @return size
   */
  int size();

  /**
   * Write a single byte into this buffer at the current writing position.
   *
   * @param b the byte to be written
   * @return this output
   */
  BufferedOutput write(byte b);

  /**
   * Write the given source into this buffer, starting at the current writing position of this
   * buffer.
   *
   * @param source the bytes to be written into this buffer
   * @return this output
   */
  BufferedOutput write(byte[] source);

  /**
   * Write at most {@code length} bytes of the given source into this buffer, starting at the
   * current writing position of this buffer.
   *
   * @param source the bytes to be written into this buffer
   * @param offset the index within {@code source} to start writing from
   * @param length the maximum number of bytes to be written from {@code source}
   * @return this output
   */
  BufferedOutput write(byte[] source, int offset, int length);

  /**
   * Write the given {@code String} using {@code UTF-8}, starting at the current writing position.
   *
   * @param source the char sequence to write into this buffer
   * @return this output
   */
  default BufferedOutput write(@NonNull String source) {
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
  default BufferedOutput write(@NonNull String source, @NonNull Charset charset) {
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
  default BufferedOutput write(@NonNull ByteBuffer source) {
    if (source.hasArray()) {
      return write(source.array(), source.arrayOffset() + source.position(), source.remaining());
    } else {
      var bytes = new byte[source.remaining()];
      source.get(bytes);
      return write(bytes);
    }
  }

  default BufferedOutput write(@NonNull CharBuffer source, @NonNull Charset charset) {
    if (!source.isEmpty()) {
      return write(charset.encode(source));
    }
    return this;
  }

  void send(io.jooby.Context ctx);

  BufferedOutput clear();
}
