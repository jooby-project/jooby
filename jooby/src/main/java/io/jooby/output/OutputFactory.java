/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.output;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Factory class for {@link Output}.
 *
 * @author edgar
 * @since 4.0.0
 */
public interface OutputFactory {

  /**
   * Default output factory, backed by {@link ByteBuffer}.
   *
   * @param options Output options.
   * @return Default output factory.
   */
  static OutputFactory create(@NonNull OutputOptions options) {
    return new ByteBufferedOutputFactory(options);
  }

  /**
   * Default output factory, backed by {@link ByteBuffer}.
   *
   * @return Default output factory.
   */
  static OutputFactory create() {
    return create(new OutputOptions());
  }

  /**
   * Configuration options.
   *
   * @return Configuration options.
   */
  OutputOptions getOptions();

  /**
   * Creates a new byte buffered output.
   *
   * @param direct True for direct buffers.
   * @param size Output size.
   * @return A byte buffered output.
   */
  BufferedOutput allocate(boolean direct, int size);

  /**
   * Creates a new byte buffered output.
   *
   * @param size Output size.
   * @return A byte buffered output.
   */
  default BufferedOutput allocate(int size) {
    return allocate(getOptions().isDirectBuffers(), size);
  }

  default BufferedOutput allocate() {
    return allocate(getOptions().isDirectBuffers(), getOptions().getSize());
  }

  /**
   * A virtual buffer which shows multiple buffers as a single merged buffer. Useful for chunk of
   * data.
   *
   * @return A new composite buffer.
   */
  BufferedOutput newComposite();

  /**
   * Readonly buffer created from string utf-8 bytes.
   *
   * @param value String.
   * @return Readonly buffer.
   */
  default Output wrap(String value) {
    return wrap(value, StandardCharsets.UTF_8);
  }

  /**
   * Readonly buffer created from string.
   *
   * @param value String.
   * @param charset Charset to use.
   * @return Readonly buffer.
   */
  default Output wrap(@NonNull String value, @NonNull Charset charset) {
    return wrap(value.getBytes(charset));
  }

  /**
   * Readonly buffer created from buffer.
   *
   * @param buffer Input buffer.
   * @return Readonly buffer.
   */
  Output wrap(@NonNull ByteBuffer buffer);

  /**
   * Readonly buffer created from byte array.
   *
   * @param bytes Byte array.
   * @return Readonly buffer.
   */
  Output wrap(@NonNull byte[] bytes);

  /**
   * Readonly buffer created from byte array.
   *
   * @param bytes Byte array.
   * @param offset Array offset.
   * @param length Length.
   * @return Readonly buffer.
   */
  Output wrap(@NonNull byte[] bytes, int offset, int length);

  /**
   * Special implementation when output factory is requested from {@link io.jooby.Context}.
   *
   * @return Same or custom implementation.
   */
  OutputFactory getContextFactory();
}
