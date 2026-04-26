/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

public class WebSocketMessageTest {

  @Test
  public void createFromBytes() {
    Context ctx = mock(Context.class);
    byte[] data = "hello bytes".getBytes(StandardCharsets.UTF_8);

    WebSocketMessage message = WebSocketMessage.create(ctx, data);

    assertNotNull(message);
    assertArrayEquals(data, message.bytes());
    // Verify it decodes correctly as a Value
    assertEquals("hello bytes", message.value());
  }

  @Test
  public void createFromString() {
    Context ctx = mock(Context.class);
    String text = "hello string";

    WebSocketMessage message = WebSocketMessage.create(ctx, text);

    assertNotNull(message);
    assertEquals(text, message.value());
    assertArrayEquals(text.getBytes(StandardCharsets.UTF_8), message.bytes());
  }

  @Test
  public void checkByteBuffer() {
    Context ctx = mock(Context.class);
    byte[] data = "buffer".getBytes(StandardCharsets.UTF_8);

    WebSocketMessage message = WebSocketMessage.create(ctx, data);

    assertNotNull(message.byteBuffer());
    assertEquals(data.length, message.byteBuffer().remaining());

    byte[] result = new byte[data.length];
    message.byteBuffer().get(result);
    assertArrayEquals(data, result);
  }
}
