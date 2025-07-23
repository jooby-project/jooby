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

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.internal.output.OutputOutputStream;
import io.jooby.internal.output.OutputWriter;

/**
 * Buffered output.
 *
 * <p>The capacity of a {@code BufferedOutput} is expanded on demand, similar to {@code
 * StringBuilder}.
 *
 * <p>The main purpose of the {@code BufferedOutput} abstraction is to provide a convenient wrapper
 * around {@link ByteBuffer} which is similar to Netty's {@code ByteBuf} but can also be used on
 * non-Netty platforms.
 *
 * @author edgar
 * @since 4.0.0
 */
public interface BufferedOutput extends Output {
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

  /**
   * Write the given source into this buffer, starting at the current writing position of this
   * buffer.
   *
   * @param source the bytes to be written into this buffer
   * @param charset Charset.
   * @return this output
   */
  default BufferedOutput write(@NonNull CharBuffer source, @NonNull Charset charset) {
    if (!source.isEmpty()) {
      return write(charset.encode(source));
    }
    return this;
  }

  /**
   * Clears this buffer. The position is set to zero, the limit is set to the capacity, and the mark
   * is discarded.
   *
   * <p>This method does not erase the data in the buffer, but it is named as if it did because it
   * will most often be used in situations in which that might as well be the case.
   *
   * @return This output.
   */
  BufferedOutput clear();
}
