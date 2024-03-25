/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.buffer;

/**
 * Extension of {@link DataBuffer} that allows for buffers that share a memory pool. Introduces
 * methods for reference counting.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public interface PooledDataBuffer extends TouchableDataBuffer {

  /**
   * Return {@code true} if this buffer is allocated; {@code false} if it has been deallocated.
   *
   * @since 5.1
   */
  boolean isAllocated();

  /**
   * Increase the reference count for this buffer by one.
   *
   * @return this buffer
   */
  PooledDataBuffer retain();

  /**
   * Associate the given hint with the data buffer for debugging purposes.
   *
   * @return this buffer
   * @since 5.3.2
   */
  @Override
  PooledDataBuffer touch(Object hint);

  /**
   * Decrease the reference count for this buffer by one, and deallocate it once the count reaches
   * zero.
   *
   * @return {@code true} if the buffer was deallocated; {@code false} otherwise
   */
  boolean release();
}
