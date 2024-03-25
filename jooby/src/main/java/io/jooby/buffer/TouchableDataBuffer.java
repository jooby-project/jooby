/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.buffer;

/**
 * Extension of {@link DataBuffer} that allows for buffers that can be given hints for debugging
 * purposes.
 *
 * @author Arjen Poutsma
 * @since 6.0
 */
public interface TouchableDataBuffer extends DataBuffer {

  /**
   * Associate the given hint with the data buffer for debugging purposes.
   *
   * @return this buffer
   */
  TouchableDataBuffer touch(Object hint);
}
