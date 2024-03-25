/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.buffer;

/**
 * Extension of {@link DataBuffer} that allows for buffers that can be used in a {@code
 * try}-with-resources statement.
 *
 * @author Arjen Poutsma
 * @since 6.0
 */
public interface CloseableDataBuffer extends DataBuffer, AutoCloseable {

  /**
   * Closes this data buffer, freeing any resources.
   *
   * @throws IllegalStateException if this buffer has already been closed
   */
  @Override
  void close();
}
