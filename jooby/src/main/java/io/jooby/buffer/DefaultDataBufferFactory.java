/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.buffer;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Default implementation of the {@code DataBufferFactory} interface. Allows for specification of
 * the default initial capacity at construction time, as well as whether heap-based or direct
 * buffers are to be preferred.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public class DefaultDataBufferFactory implements DataBufferFactory {

  /**
   * The default capacity when none is specified.
   *
   * @see #DefaultDataBufferFactory()
   * @see #DefaultDataBufferFactory(boolean)
   */
  public static final int DEFAULT_INITIAL_CAPACITY = 4096;

  /**
   * Shared instance based on the default constructor.
   *
   * @since 5.3
   */
  public static final DefaultDataBufferFactory sharedInstance = new DefaultDataBufferFactory();

  private final boolean preferDirect;

  private final int defaultInitialCapacity;

  /**
   * Creates a new {@code DefaultDataBufferFactory} with default settings.
   *
   * @see #sharedInstance
   */
  public DefaultDataBufferFactory() {
    this(false);
  }

  /**
   * Creates a new {@code DefaultDataBufferFactory}, indicating whether direct buffers should be
   * created by {@link #allocateBuffer()} and {@link #allocateBuffer(int)}.
   *
   * @param preferDirect {@code true} if direct buffers are to be preferred; {@code false} otherwise
   */
  public DefaultDataBufferFactory(boolean preferDirect) {
    this(preferDirect, DEFAULT_INITIAL_CAPACITY);
  }

  /**
   * Creates a new {@code DefaultDataBufferFactory}, indicating whether direct buffers should be
   * created by {@link #allocateBuffer()} and {@link #allocateBuffer(int)}, and what the capacity is
   * to be used for {@link #allocateBuffer()}.
   *
   * @param preferDirect {@code true} if direct buffers are to be preferred; {@code false} otherwise
   */
  public DefaultDataBufferFactory(boolean preferDirect, int defaultInitialCapacity) {
    Assert.isTrue(defaultInitialCapacity > 0, "'defaultInitialCapacity' should be larger than 0");
    this.preferDirect = preferDirect;
    this.defaultInitialCapacity = defaultInitialCapacity;
  }

  @Override
  public DefaultDataBuffer allocateBuffer() {
    return allocateBuffer(this.defaultInitialCapacity);
  }

  @Override
  public DefaultDataBuffer allocateBuffer(int initialCapacity) {
    ByteBuffer byteBuffer =
        (this.preferDirect
            ? ByteBuffer.allocateDirect(initialCapacity)
            : ByteBuffer.allocate(initialCapacity));
    return DefaultDataBuffer.fromEmptyByteBuffer(this, byteBuffer);
  }

  @Override
  public DefaultDataBuffer wrap(ByteBuffer byteBuffer) {
    return DefaultDataBuffer.fromFilledByteBuffer(this, byteBuffer.slice());
  }

  @Override
  public DefaultDataBuffer wrap(byte[] bytes) {
    return wrap(bytes, 0, bytes.length);
  }

  @Override
  public DefaultDataBuffer wrap(byte[] bytes, int offset, int length) {
    return DefaultDataBuffer.fromFilledByteBuffer(this, ByteBuffer.wrap(bytes, offset, length));
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation creates a single {@link DefaultDataBuffer} to contain the data in {@code
   * dataBuffers}.
   */
  @Override
  public DefaultDataBuffer join(List<? extends DataBuffer> dataBuffers) {
    Assert.notEmpty(dataBuffers, "DataBuffer List must not be empty");
    int capacity = dataBuffers.stream().mapToInt(DataBuffer::readableByteCount).sum();
    DefaultDataBuffer result = allocateBuffer(capacity);
    dataBuffers.forEach(result::write);
    dataBuffers.forEach(DataBufferUtils::release);
    return result;
  }

  @Override
  public boolean isDirect() {
    return this.preferDirect;
  }

  @Override
  public String toString() {
    return "DefaultDataBufferFactory (preferDirect=" + this.preferDirect + ")";
  }
}
