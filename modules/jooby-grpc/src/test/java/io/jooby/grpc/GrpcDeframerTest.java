/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.grpc;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jooby.internal.grpc.GrpcDeframer;

public class GrpcDeframerTest {

  private GrpcDeframer deframer;
  private List<byte[]> outputMessages;

  @BeforeEach
  public void setUp() {
    deframer = new GrpcDeframer();
    outputMessages = new ArrayList<>();
  }

  @Test
  public void shouldParseSingleCompleteMessage() {
    byte[] payload = "hello grpc".getBytes();
    ByteBuffer frame = createGrpcFrame(payload);

    deframer.process(frame, msg -> outputMessages.add(msg));

    assertEquals(1, outputMessages.size());
    assertArrayEquals(payload, outputMessages.get(0));
  }

  @Test
  public void shouldParseFragmentedHeader() {
    byte[] payload = "fragmented header".getBytes();
    byte[] frame = createGrpcFrame(payload).array();

    // Send the first 2 bytes of the header
    deframer.process(ByteBuffer.wrap(frame, 0, 2), msg -> outputMessages.add(msg));
    assertEquals(0, outputMessages.size(), "Should not emit message yet");

    // Send the rest of the header and the payload
    deframer.process(ByteBuffer.wrap(frame, 2, frame.length - 2), msg -> outputMessages.add(msg));

    assertEquals(1, outputMessages.size());
    assertArrayEquals(payload, outputMessages.get(0));
  }

  @Test
  public void shouldParseFragmentedPayload() {
    byte[] payload = "this is a very long payload that gets split".getBytes();
    byte[] frame = createGrpcFrame(payload).array();

    // Send the 5-byte header + first 10 bytes of payload (15 bytes total)
    deframer.process(ByteBuffer.wrap(frame, 0, 15), msg -> outputMessages.add(msg));
    assertEquals(0, outputMessages.size(), "Should not emit message until full payload arrives");

    // Send the remainder of the payload
    deframer.process(ByteBuffer.wrap(frame, 15, frame.length - 15), msg -> outputMessages.add(msg));

    assertEquals(1, outputMessages.size());
    assertArrayEquals(payload, outputMessages.get(0));
  }

  @Test
  public void shouldParseMultipleMessagesInSingleBuffer() {
    byte[] payload1 = "message 1".getBytes();
    byte[] payload2 = "message 2".getBytes();

    ByteBuffer frame1 = createGrpcFrame(payload1);
    ByteBuffer frame2 = createGrpcFrame(payload2);

    // Combine both frames into a single buffer
    ByteBuffer combined = ByteBuffer.allocate(frame1.capacity() + frame2.capacity());
    combined.put(frame1).put(frame2);
    combined.flip();

    deframer.process(combined, msg -> outputMessages.add(msg));

    assertEquals(2, outputMessages.size());
    assertArrayEquals(payload1, outputMessages.get(0));
    assertArrayEquals(payload2, outputMessages.get(1));
  }

  @Test
  public void shouldHandleZeroLengthPayload() {
    byte[] payload = new byte[0];
    ByteBuffer frame = createGrpcFrame(payload);

    deframer.process(frame, msg -> outputMessages.add(msg));

    assertEquals(1, outputMessages.size());
    assertArrayEquals(payload, outputMessages.get(0));
  }

  @Test
  public void shouldHandleExtremeFragmentationByteByByte() {
    byte[] payload = "byte by byte".getBytes();
    byte[] frame = createGrpcFrame(payload).array();

    for (byte b : frame) {
      deframer.process(ByteBuffer.wrap(new byte[] {b}), msg -> outputMessages.add(msg));
    }

    assertEquals(1, outputMessages.size());
    assertArrayEquals(payload, outputMessages.get(0));
  }

  /** Helper to wrap a raw payload in the standard 5-byte gRPC framing. */
  private ByteBuffer createGrpcFrame(byte[] payload) {
    ByteBuffer buffer = ByteBuffer.allocate(5 + payload.length);
    buffer.put((byte) 0); // Compressed flag
    buffer.putInt(payload.length); // Length
    buffer.put(payload); // Data
    buffer.flip();
    return buffer;
  }
}
