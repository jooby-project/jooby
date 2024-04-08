/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.buffer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.jooby.netty.buffer.NettyDataBufferFactory;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;

/**
 * @author Arjen Poutsma
 * @author Sam Brannen
 */
class PooledDataBufferTests {

  @Nested
  class UnpooledByteBufAllocatorWithPreferDirectTrueTests implements PooledDataBufferTestingTrait {

    @Override
    public DataBufferFactory createDataBufferFactory() {
      return new NettyDataBufferFactory(new UnpooledByteBufAllocator(true));
    }
  }

  @Nested
  class UnpooledByteBufAllocatorWithPreferDirectFalseTests implements PooledDataBufferTestingTrait {

    @Override
    public DataBufferFactory createDataBufferFactory() {
      return new NettyDataBufferFactory(new UnpooledByteBufAllocator(true));
    }
  }

  @Nested
  class PooledByteBufAllocatorWithPreferDirectTrueTests implements PooledDataBufferTestingTrait {

    @Override
    public DataBufferFactory createDataBufferFactory() {
      return new NettyDataBufferFactory(new PooledByteBufAllocator(true));
    }
  }

  @Nested
  class PooledByteBufAllocatorWithPreferDirectFalseTests implements PooledDataBufferTestingTrait {

    @Override
    public DataBufferFactory createDataBufferFactory() {
      return new NettyDataBufferFactory(new PooledByteBufAllocator(true));
    }
  }

  interface PooledDataBufferTestingTrait {

    DataBufferFactory createDataBufferFactory();

    default PooledDataBuffer createDataBuffer(int capacity) {
      return (PooledDataBuffer) createDataBufferFactory().allocateBuffer(capacity);
    }

    @Test
    default void retainAndRelease() {
      PooledDataBuffer buffer = createDataBuffer(1);
      buffer.write((byte) 'a');

      buffer.retain();
      assertThat(buffer.release()).isFalse();
      assertThat(buffer.release()).isTrue();
    }

    @Test
    default void tooManyReleases() {
      PooledDataBuffer buffer = createDataBuffer(1);
      buffer.write((byte) 'a');

      buffer.release();
      assertThatIllegalStateException().isThrownBy(buffer::release);
    }
  }
}
