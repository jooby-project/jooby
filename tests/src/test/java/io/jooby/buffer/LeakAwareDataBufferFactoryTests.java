/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.buffer;

import static io.jooby.buffer.DataBufferUtils.release;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;

/**
 * @author Arjen Poutsma
 */
class LeakAwareDataBufferFactoryTests {

  private final LeakAwareDataBufferFactory bufferFactory = new LeakAwareDataBufferFactory();

  @Test
  @SuppressWarnings("deprecation")
  void leak() {
    DataBuffer dataBuffer = this.bufferFactory.allocateBuffer();
    try {
      assertThatExceptionOfType(AssertionError.class).isThrownBy(this.bufferFactory::checkForLeaks);
    } finally {
      release(dataBuffer);
    }
  }

  @Test
  void noLeak() {
    DataBuffer dataBuffer = this.bufferFactory.allocateBuffer(256);
    release(dataBuffer);
    this.bufferFactory.checkForLeaks();
  }
}
