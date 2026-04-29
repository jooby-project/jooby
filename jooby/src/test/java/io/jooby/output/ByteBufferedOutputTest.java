/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.output;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jooby.Context;

public class ByteBufferedOutputTest {

  private Method setCapacityMethod;
  private Method calculateCapacityMethod;

  @BeforeEach
  void setUp() throws Exception {
    setCapacityMethod = ByteBufferedOutput.class.getDeclaredMethod("setCapacity", int.class);
    setCapacityMethod.setAccessible(true);

    calculateCapacityMethod =
        ByteBufferedOutput.class.getDeclaredMethod("calculateCapacity", int.class);
    calculateCapacityMethod.setAccessible(true);
  }

  @Test
  @DisplayName("Verify buffer allocation for direct and heap buffers")
  void testAllocation() {
    ByteBufferedOutput heapBuffer = new ByteBufferedOutput(false, 10);
    assertFalse(heapBuffer.asByteBuffer().isDirect());

    ByteBufferedOutput directBuffer = new ByteBufferedOutput(true, 10);
    assertTrue(directBuffer.asByteBuffer().isDirect());
  }

  @Test
  @DisplayName("Verify single byte, byte array, and ByteBuffer writes")
  void testWrites() {
    ByteBufferedOutput output = new ByteBufferedOutput(false, 10);

    // Write single byte
    output.write((byte) 1);
    assertEquals(1, output.size());

    // Write byte array
    output.write(new byte[] {2, 3});
    assertEquals(3, output.size());

    // Write byte array with offset
    output.write(new byte[] {9, 4, 5, 9}, 1, 2);
    assertEquals(5, output.size());

    // Write ByteBuffer
    output.write(ByteBuffer.wrap(new byte[] {6, 7}));
    assertEquals(7, output.size());

    ByteBuffer result = output.asByteBuffer();
    assertEquals(7, result.remaining());
    byte[] extracted = new byte[7];
    result.get(extracted);
    assertArrayEquals(new byte[] {1, 2, 3, 4, 5, 6, 7}, extracted);
  }

  @Test
  @DisplayName("Verify transferTo invokes the consumer with the underlying ByteBuffer")
  void testTransferTo() {
    ByteBufferedOutput output = new ByteBufferedOutput(false, 10);
    output.write(new byte[] {1, 2, 3});

    AtomicReference<ByteBuffer> captured = new AtomicReference<>();
    output.transferTo(captured::set);

    assertEquals(3, captured.get().remaining());
    assertEquals(1, captured.get().get());
  }

  @Test
  @DisplayName("Verify iterator returns a single ByteBuffer element")
  void testIterator() {
    ByteBufferedOutput output = new ByteBufferedOutput(false, 10);
    output.write(new byte[] {1, 2});

    Iterator<ByteBuffer> iterator = output.iterator();
    assertTrue(iterator.hasNext());
    assertEquals(2, iterator.next().remaining());
    assertFalse(iterator.hasNext());
  }

  @Test
  @DisplayName("Verify clear resets read and write positions")
  void testClear() {
    ByteBufferedOutput output = new ByteBufferedOutput(false, 10);
    output.write(new byte[] {1, 2, 3});
    assertEquals(3, output.size());

    output.clear();
    assertEquals(0, output.size());
    assertEquals(0, output.asByteBuffer().remaining());
  }

  @Test
  @DisplayName("Verify send delegates the sliced buffer to the Context")
  void testSend() {
    Context ctx = mock(Context.class);
    ByteBufferedOutput output = new ByteBufferedOutput(false, 10);
    output.write(new byte[] {1, 2, 3});

    output.send(ctx);

    // Context should receive a sliced buffer containing our 3 bytes
    verify(ctx).send(output.asByteBuffer());
  }

  @Test
  @DisplayName("Verify toString output format")
  void testToString() {
    ByteBufferedOutput output = new ByteBufferedOutput(false, 10);
    output.write(new byte[] {1, 2, 3});

    String str = output.toString();
    assertTrue(str.contains("readPosition=0"));
    assertTrue(str.contains("writePosition=3"));
    assertTrue(str.contains("size=3"));
    assertTrue(str.contains("capacity="));
  }

  @Test
  @DisplayName("Verify automatic expansion when writable bytes are insufficient")
  void testAutomaticExpansion() {
    ByteBufferedOutput output = new ByteBufferedOutput(false, 2);
    // Capacity starts at 2. Writing 5 bytes triggers ensureWritable -> calculateCapacity
    output.write(new byte[] {1, 2, 3, 4, 5});

    assertEquals(5, output.size());
    // Based on the bitshift logic (64 << 1), it expands to at least 64
    assertTrue(output.toString().contains("capacity=64"));
  }

  @Test
  @DisplayName("Verify calculateCapacity edge cases via Reflection to avoid OOM")
  void testCalculateCapacityEdgeCases() throws Exception {
    ByteBufferedOutput output = new ByteBufferedOutput(false, 10);
    int CAPACITY_THRESHOLD = 1024 * 1024 * 4;

    // 1. neededCapacity == CAPACITY_THRESHOLD
    assertEquals(
        CAPACITY_THRESHOLD, (int) calculateCapacityMethod.invoke(output, CAPACITY_THRESHOLD));

    // 2. neededCapacity > CAPACITY_THRESHOLD but well within MAX
    assertEquals(
        CAPACITY_THRESHOLD * 2,
        (int) calculateCapacityMethod.invoke(output, CAPACITY_THRESHOLD + 10));

    // 3. neededCapacity > (MAX_CAPACITY - CAPACITY_THRESHOLD) -> Caps at MAX_CAPACITY
    int nearMax = Integer.MAX_VALUE - 100;
    assertEquals(Integer.MAX_VALUE, (int) calculateCapacityMethod.invoke(output, nearMax));
  }

  @Test
  @DisplayName("Verify setCapacity shrinkage branches via Reflection")
  void testSetCapacityShrinkage() throws Exception {
    ByteBufferedOutput output = new ByteBufferedOutput(false, 20);
    output.write(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10});

    // Branch 1: Shrinking where readPosition < newCapacity and writePosition > newCapacity
    // writePosition is 10. We shrink to 5.
    setCapacityMethod.invoke(output, 5);
    assertEquals(5, output.size());

    // Branch 2: Shrinking where readPosition < newCapacity is FALSE
    // readPosition is always 0. Setting capacity to 0 forces the else block.
    setCapacityMethod.invoke(output, 0);
    assertEquals(0, output.size());
  }

  @Test
  @DisplayName("Verify setCapacity throws IllegalArgumentException on negative capacity")
  void testSetCapacityNegative() {
    ByteBufferedOutput output = new ByteBufferedOutput(false, 10);

    InvocationTargetException ex =
        assertThrows(InvocationTargetException.class, () -> setCapacityMethod.invoke(output, -1));

    assertTrue(ex.getCause() instanceof IllegalArgumentException);
    assertEquals("'newCapacity' -1 must be 0 or higher", ex.getCause().getMessage());
  }
}
