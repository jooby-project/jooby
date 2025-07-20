/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.output;

import static java.lang.ThreadLocal.withInitial;

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
   * Thread local for output buffer. Please note only store calls to {@link #newOutput()}, {@link
   * #newCompositeOutput()} are not saved into thread local.
   *
   * @param factory Factory.
   * @return Thread local factory.
   */
  static OutputFactory threadLocal(OutputFactory factory) {
    return new ForwardingOutputFactory(factory) {
      private final ThreadLocal<Output> threadLocal = withInitial(factory::newOutput);

      @Override
      public Output newOutput(boolean direct, int size) {
        return threadLocal.get().clear();
      }

      @Override
      public Output newOutput(int size) {
        return threadLocal.get().clear();
      }

      @Override
      public Output newOutput() {
        return threadLocal.get().clear();
      }
    };
  }

  /**
   * Default output factory, backed by {@link ByteBuffer}.
   *
   * @return Default output factory.
   */
  static OutputFactory create(OutputOptions options) {
    return new ByteBufferOutputFactory(options);
  }

  /**
   * Default output factory, backed by {@link ByteBuffer}.
   *
   * @return Default output factory.
   */
  static OutputFactory create() {
    return create(new OutputOptions());
  }

  OutputOptions getOptions();

  OutputFactory setOptions(OutputOptions options);

  /**
   * Creates a new byte buffered output.
   *
   * @param direct True for direct buffers.
   * @param size Output size.
   * @return A byte buffered output.
   */
  Output newOutput(boolean direct, int size);

  /**
   * Creates a new byte buffered output.
   *
   * @param size Output size.
   * @return A byte buffered output.
   */
  default Output newOutput(int size) {
    return newOutput(getOptions().isDirectBuffers(), size);
  }

  default Output newOutput() {
    return newOutput(getOptions().isDirectBuffers(), getOptions().getSize());
  }

  /**
   * A virtual buffer which shows multiple buffers as a single merged buffer. Useful for chunk of
   * data.
   *
   * @return A new composite buffer.
   */
  Output newCompositeOutput();

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
}
