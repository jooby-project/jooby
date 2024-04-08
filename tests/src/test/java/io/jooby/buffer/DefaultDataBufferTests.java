/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.buffer;

import static io.jooby.buffer.DataBufferUtils.release;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DefaultDataBuffer}.
 *
 * @author Injae Kim
 * @since 6.2
 */
class DefaultDataBufferTests {

  private final DefaultDataBufferFactory bufferFactory = new DefaultDataBufferFactory();

  @Test // gh-30967
  void getNativeBuffer() {
    DefaultDataBuffer dataBuffer = this.bufferFactory.allocateBuffer(256);
    dataBuffer.write("0123456789", StandardCharsets.UTF_8);

    byte[] result = new byte[7];
    dataBuffer.read(result);
    assertThat(result).isEqualTo("0123456".getBytes(StandardCharsets.UTF_8));

    ByteBuffer nativeBuffer = dataBuffer.getNativeBuffer();
    assertThat(nativeBuffer.position()).isEqualTo(7);
    assertThat(dataBuffer.readPosition()).isEqualTo(7);
    assertThat(nativeBuffer.limit()).isEqualTo(10);
    assertThat(dataBuffer.writePosition()).isEqualTo(10);
    assertThat(nativeBuffer.capacity()).isEqualTo(256);
    assertThat(dataBuffer.capacity()).isEqualTo(256);

    release(dataBuffer);
  }
}
