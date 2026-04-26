/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jooby.Body;
import io.jooby.Context;
import io.jooby.MediaType;
import io.jooby.value.Value;
import io.jooby.value.ValueFactory;

public class ByteArrayBodyTest {

  private Context ctx;
  private byte[] content = "jooby".getBytes(StandardCharsets.UTF_8);
  private ByteArrayBody body;

  @BeforeEach
  void setUp() {
    ctx = mock(Context.class);
    body = new ByteArrayBody(ctx, content);
  }

  @Test
  void testMetadata() {
    assertEquals(content.length, body.getSize());
    assertTrue(body.isInMemory());
    assertEquals("body", body.name());
    assertEquals(List.of("jooby"), body.toList());
    assertTrue(body.toMultimap().isEmpty());
  }

  @Test
  void testContentAccess() throws Exception {
    // Bytes
    assertArrayEquals(content, body.bytes());

    // Stream
    try (InputStream is = body.stream()) {
      assertArrayEquals(content, is.readAllBytes());
    }

    // Channel
    try (ReadableByteChannel channel = body.channel()) {
      assertTrue(channel.isOpen());
    }

    // String value
    assertEquals("jooby", body.value());
  }

  @Test
  void testValueMethods() {
    ValueFactory vf = mock(ValueFactory.class);
    when(ctx.getValueFactory()).thenReturn(vf);

    // get (Missing)
    Value missing = body.get("any");
    assertTrue(missing.isMissing());
  }

  @Test
  void testTypeConversion() {
    MediaType text = MediaType.text;
    when(ctx.getRequestType(text)).thenReturn(text);
    when(ctx.decode(String.class, text)).thenReturn("decoded");

    // to
    assertEquals("decoded", body.to(String.class));

    // toNullable (Non-empty)
    assertEquals("decoded", body.toNullable(String.class));

    // toNullable (Empty)
    ByteArrayBody emptyBody = new ByteArrayBody(ctx, new byte[0]);
    assertNull(emptyBody.toNullable(String.class));
  }

  @Test
  void testEmptyFactory() {
    Body empty = ByteArrayBody.empty(ctx);
    assertEquals(0, empty.getSize());
    assertArrayEquals(new byte[0], empty.bytes());
  }
}
