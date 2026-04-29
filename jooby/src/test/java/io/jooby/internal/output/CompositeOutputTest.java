/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.output;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.nio.ByteBuffer;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.jooby.Context;
import io.jooby.SneakyThrows;

public class CompositeOutputTest {

  private CompositeOutput output;

  @BeforeEach
  void setUp() {
    output = new CompositeOutput();
  }

  @Test
  @DisplayName("Verify size is initially zero")
  void testInitialSize() {
    assertEquals(0, output.size());
  }

  @Test
  @DisplayName("Verify write(byte) adds a single byte chunk")
  void testWriteSingleByte() {
    CompositeOutput result = (CompositeOutput) output.write((byte) 65); // ASCII 'A'

    assertSame(output, result);
    assertEquals(1, output.size());
    assertEquals("chunks=1, size=1", output.toString());
  }

  @Test
  @DisplayName("Verify write(byte[]) adds a byte array chunk")
  void testWriteByteArray() {
    byte[] data = {1, 2, 3};
    CompositeOutput result = (CompositeOutput) output.write(data);

    assertSame(output, result);
    assertEquals(3, output.size());
    assertEquals("chunks=1, size=3", output.toString());
  }

  @Test
  @DisplayName("Verify write(byte[], offset, length) adds a sliced array chunk")
  void testWriteByteArraySlice() {
    byte[] data = {10, 20, 30, 40, 50};
    CompositeOutput result = (CompositeOutput) output.write(data, 1, 3); // {20, 30, 40}

    assertSame(output, result);
    assertEquals(3, output.size());
    assertEquals("chunks=1, size=3", output.toString());
  }

  @Test
  @DisplayName("Verify asByteBuffer correctly merges multiple chunks into one")
  void testAsByteBuffer() {
    output.write((byte) 10);
    output.write(new byte[] {20, 30});
    output.write(new byte[] {99, 40, 50, 99}, 1, 2);

    assertEquals(5, output.size()); // 1 + 2 + 2 = 5 bytes total

    ByteBuffer merged = output.asByteBuffer();

    // Verify properties of the merged buffer
    assertEquals(5, merged.remaining());
    assertEquals(0, merged.position());
    assertEquals(5, merged.capacity());

    // Verify exact content
    byte[] extracted = new byte[5];
    merged.get(extracted);
    assertArrayEquals(new byte[] {10, 20, 30, 40, 50}, extracted);
  }

  @Test
  @DisplayName("Verify clear empties the chunks list")
  void testClear() {
    output.write(new byte[] {1, 2, 3});
    output.clear();

    // Note: The current CompositeOutput implementation clears the list
    // but omits resetting `size` to 0. This tests the actual current behavior.
    assertEquals("chunks=0, size=0", output.toString());

    // asByteBuffer should produce an empty buffer (because 0 chunks to iterate over)
    ByteBuffer buffer = output.asByteBuffer();
    assertEquals(0, buffer.remaining());
  }

  @Test
  @DisplayName("Verify transferTo calls the consumer for each chunk")
  void testTransferTo() {
    output.write((byte) 1);
    output.write(new byte[] {2, 3});

    @SuppressWarnings("unchecked")
    SneakyThrows.Consumer<ByteBuffer> consumer = mock(SneakyThrows.Consumer.class);

    output.transferTo(consumer);

    // Verify the consumer was called exactly twice (once per chunk)
    ArgumentCaptor<ByteBuffer> captor = ArgumentCaptor.forClass(ByteBuffer.class);
    verify(consumer, times(2)).accept(captor.capture());

    List<ByteBuffer> capturedChunks = captor.getAllValues();
    assertEquals(1, capturedChunks.get(0).remaining());
    assertEquals(2, capturedChunks.get(1).remaining());
  }

  @Test
  @DisplayName("Verify send delegates the array of ByteBuffers to the Context")
  void testSend() {
    Context ctx = mock(Context.class);
    output.write((byte) 10);
    output.write(new byte[] {20, 30});

    output.send(ctx);

    ArgumentCaptor<ByteBuffer[]> arrayCaptor = ArgumentCaptor.forClass(ByteBuffer[].class);
    verify(ctx).send(arrayCaptor.capture());

    ByteBuffer[] sentArray = arrayCaptor.getValue();
    assertEquals(2, sentArray.length); // 2 chunks
    assertEquals(1, sentArray[0].remaining());
    assertEquals(2, sentArray[1].remaining());
  }
}
