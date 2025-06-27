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
 * Factory class for buffered {@link Output}.
 *
 * @author edgar
 * @since 4.0.0
 */
public interface OutputFactory {

  /**
   * Thread local for output buffer. Please note only store calls to {@link #newBufferedOutput()},
   * {@link #newCompositeOutput()} are not saved into thread local.
   *
   * @param factory Factory.
   * @return Thread local factory.
   */
  static OutputFactory threadLocal(OutputFactory factory) {
    return new ForwardingOutputFactory(factory) {
      private final ThreadLocal<Output> threadLocal = withInitial(factory::newBufferedOutput);

      @Override
      public Output newBufferedOutput(boolean direct, int size) {
        return threadLocal.get().clear();
      }

      @Override
      public Output newBufferedOutput(int size) {
        return threadLocal.get().clear();
      }

      @Override
      public Output newBufferedOutput() {
        return threadLocal.get().clear();
      }
    };
  }

  /**
   * Default output factory, backed by {@link ByteBuffer}.
   *
   * @return Default output factory.
   */
  static OutputFactory create(boolean direct, int bufferSize) {
    return new ByteBufferOutputFactory(direct, bufferSize);
  }

  /**
   * Default output factory, backed by {@link ByteBuffer}.
   *
   * @return Default output factory.
   */
  static OutputFactory create(boolean direct) {
    return new ByteBufferOutputFactory(direct, Output.BUFFER_SIZE);
  }

  /**
   * Indicates whether this factory allocates direct buffers (i.e. non-heap, native memory).
   *
   * @return {@code true} if this factory allocates direct buffers; {@code false} otherwise
   */
  boolean isDirect();

  /**
   * Buffer of a default initial capacity. Default capacity is <code>1024</code> bytes.
   *
   * @return buffer of a default initial capacity.
   */
  int getInitialBufferSize();

  /**
   * Set default buffer initial capacity.
   *
   * @param initialBufferSize Default initial buffer capacity.
   * @return This buffer factory.
   */
  OutputFactory setInitialBufferSize(int initialBufferSize);

  /**
   * Creates a new byte buffered output.
   *
   * @param direct True for direct buffers.
   * @param size Output size.
   * @return A byte buffered output.
   */
  Output newBufferedOutput(boolean direct, int size);

  /**
   * Creates a new byte buffered output with an initial size of {@link Output#BUFFER_SIZE}.
   *
   * @param size Output size.
   * @return A byte buffered output.
   */
  default Output newBufferedOutput(int size) {
    return newBufferedOutput(isDirect(), size);
  }

  default Output newBufferedOutput() {
    return newBufferedOutput(isDirect(), Output.BUFFER_SIZE);
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
