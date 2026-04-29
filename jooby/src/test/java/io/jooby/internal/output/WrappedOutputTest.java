/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.output;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jooby.Context;

class WrappedOutputTest {

  @Test
  @DisplayName("Verify size returns the remaining bytes in the buffer")
  void testSize() {
    ByteBuffer buffer = ByteBuffer.allocate(10);
    buffer.position(3);
    buffer.limit(8);

    WrappedOutput output = new WrappedOutput(buffer);

    // 8 - 3 = 5
    assertEquals(5, output.size());
  }

  @Test
  @DisplayName("Verify asByteBuffer returns a slice of the original buffer")
  void testAsByteBuffer() {
    ByteBuffer buffer = ByteBuffer.wrap(new byte[] {10, 20, 30});
    WrappedOutput output = new WrappedOutput(buffer);

    ByteBuffer slice = output.asByteBuffer();

    // Slices are different objects but share the same data
    assertNotSame(buffer, slice);
    assertEquals(3, slice.remaining());
    assertEquals(10, slice.get());
  }

  @Test
  @DisplayName("Verify transferTo invokes the consumer with a buffer slice")
  void testTransferTo() {
    ByteBuffer buffer = ByteBuffer.wrap(new byte[] {1, 2});
    WrappedOutput output = new WrappedOutput(buffer);

    AtomicReference<ByteBuffer> captured = new AtomicReference<>();
    output.transferTo(captured::set);

    assertEquals(2, captured.get().remaining());
    assertEquals(1, captured.get().get());
  }

  @Test
  @DisplayName("Verify toString format")
  void testToString() {
    ByteBuffer buffer = ByteBuffer.allocate(50);
    WrappedOutput output = new WrappedOutput(buffer);

    assertEquals("size=50", output.toString());
  }

  @Test
  @DisplayName(
      "Verify send delegates the raw buffer to the Context (unlike OutputStatic which slices)")
  void testSend() {
    Context ctx = mock(Context.class);
    ByteBuffer buffer = ByteBuffer.allocate(5);
    WrappedOutput output = new WrappedOutput(buffer);

    output.send(ctx);

    // WrappedOutput is designed to pass the original buffer reference to the context
    verify(ctx).send(buffer);
  }

  @Test
  @DisplayName("Verify record properties (equals, hashCode, and accessor)")
  void testRecordProperties() {
    ByteBuffer buffer = ByteBuffer.allocate(5);
    WrappedOutput output1 = new WrappedOutput(buffer);
    WrappedOutput output2 = new WrappedOutput(buffer);

    assertEquals(output1, output2);
    assertEquals(output1.hashCode(), output2.hashCode());
    assertSame(buffer, output1.buffer());
  }
}
