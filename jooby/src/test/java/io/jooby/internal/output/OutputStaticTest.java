/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.output;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jooby.Context;

class OutputStaticTest {

  @Test
  @DisplayName("Verify size returns the remaining bytes in the buffer")
  void testSize() {
    ByteBuffer buffer = ByteBuffer.allocate(10);
    buffer.position(2);
    buffer.limit(5);

    OutputStatic output = new OutputStatic(buffer);

    // 5 - 2 = 3
    assertEquals(3, output.size());
  }

  @Test
  @DisplayName("Verify asByteBuffer returns a slice of the original buffer")
  void testAsByteBuffer() {
    ByteBuffer buffer = ByteBuffer.wrap(new byte[] {1, 2, 3, 4, 5});
    buffer.position(1); // Value at index 1 is '2'

    OutputStatic output = new OutputStatic(buffer);
    ByteBuffer slice = output.asByteBuffer();

    // Slices share the same content but have independent position/limit
    assertNotSame(buffer, slice);
    assertEquals(4, slice.remaining());
    // The first byte of the slice is the value at the original buffer's position
    assertEquals(2, slice.get());
  }

  @Test
  @DisplayName("Verify transferTo invokes the consumer with a buffer slice")
  void testTransferTo() {
    ByteBuffer buffer = ByteBuffer.wrap(new byte[] {10, 20});
    OutputStatic output = new OutputStatic(buffer);

    AtomicReference<ByteBuffer> captured = new AtomicReference<>();
    output.transferTo(captured::set);

    assertNotNull(captured.get());
    assertEquals(2, captured.get().remaining());
    assertEquals(10, captured.get().get());
  }

  @Test
  @DisplayName("Verify toString format")
  void testToString() {
    ByteBuffer buffer = ByteBuffer.allocate(100);
    OutputStatic output = new OutputStatic(buffer);

    assertEquals("size=100", output.toString());
  }

  @Test
  @DisplayName("Verify send delegates a slice of the buffer to the Context")
  void testSend() {
    Context ctx = mock(Context.class);
    ByteBuffer buffer = ByteBuffer.wrap(new byte[] {1, 2, 3});
    OutputStatic output = new OutputStatic(buffer);

    output.send(ctx);

    // Context should receive a slice, not the raw buffer reference
    verify(ctx).send(output.asByteBuffer());
  }

  @Test
  @DisplayName("Verify record properties (equals, hashCode, and accessor)")
  void testRecordProperties() {
    ByteBuffer buffer = ByteBuffer.allocate(5);
    OutputStatic output1 = new OutputStatic(buffer);
    OutputStatic output2 = new OutputStatic(buffer);

    assertEquals(output1, output2);
    assertEquals(output1.hashCode(), output2.hashCode());
    assertSame(buffer, output1.buffer());
  }
}
