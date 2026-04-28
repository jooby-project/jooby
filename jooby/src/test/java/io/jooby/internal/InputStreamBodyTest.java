/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jooby.Context;
import io.jooby.MediaType;
import io.jooby.value.Value;
import io.jooby.value.ValueFactory;

public class InputStreamBodyTest {

  @Test
  @DisplayName("Test basic getters and standard properties")
  void testBasicProperties() throws Exception {
    Context ctx = mock(Context.class);
    InputStream stream = new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8));
    InputStreamBody body = new InputStreamBody(ctx, stream, 4L);

    assertFalse(body.isInMemory());
    assertEquals(4L, body.getSize());
    assertEquals(stream, body.stream());
    assertEquals("body", body.name());
    assertEquals(Collections.emptyMap(), body.toMultimap());

    ReadableByteChannel channel = body.channel();
    assertNotNull(channel);
    assertTrue(channel.isOpen());
    channel.close();
  }

  @Test
  @DisplayName("Test bytes() array read loop directly")
  void testBytesExplicitly() {
    Context ctx = mock(Context.class);
    byte[] data = "explicit bytes".getBytes(StandardCharsets.UTF_8);
    InputStream stream = new ByteArrayInputStream(data);
    InputStreamBody body = new InputStreamBody(ctx, stream, data.length);

    byte[] result = body.bytes();
    assertArrayEquals(data, result);
  }

  @Test
  @DisplayName("Test bytes() IOException propagation")
  void testBytesException() {
    Context ctx = mock(Context.class);
    InputStream errorStream =
        new InputStream() {
          @Override
          public int read() throws IOException {
            throw new IOException("Stream failure");
          }

          @Override
          public int read(byte[] b, int off, int len) throws IOException {
            throw new IOException("Stream failure");
          }
        };
    InputStreamBody body = new InputStreamBody(ctx, errorStream, 0L);

    // Verify the catch block successfully wraps the checked IOException via SneakyThrows
    assertThrows(IOException.class, body::bytes);
  }

  @Test
  @DisplayName("Test value() and toList() reading from stream")
  void testValueAndToList() {
    Context ctx = mock(Context.class);
    InputStream stream = new ByteArrayInputStream("hello world".getBytes(StandardCharsets.UTF_8));
    InputStreamBody body = new InputStreamBody(ctx, stream, 11L);

    // toList() calls value(), which delegates to the default value(Charset) decoding bytes()
    List<String> list = body.toList();
    assertEquals(1, list.size());
    assertEquals("hello world", list.get(0));
  }

  @Test
  @DisplayName("Test get() and getOrDefault() for Value API")
  void testValueGetters() {
    Context ctx = mock(Context.class);
    ValueFactory factory = new ValueFactory();
    when(ctx.getValueFactory()).thenReturn(factory);

    InputStreamBody body = new InputStreamBody(ctx, new ByteArrayInputStream(new byte[0]), 0L);

    // get() returns a MissingValue node
    Value missing = body.get("anyKey");
    assertTrue(missing.isMissing());
    assertEquals("anyKey", missing.name());

    // getOrDefault()
    Value def = body.getOrDefault("someKey", "defaultVal");
    assertEquals("defaultVal", def.value());
  }

  @Test
  @DisplayName("Test to() and toNullable() decoding delegation")
  void testToAndToNullable() {
    Context ctx = mock(Context.class);
    MediaType textType = MediaType.text;

    // Mock the context routing the payload body to the registered body decoder
    when(ctx.getRequestType(MediaType.text)).thenReturn(textType);
    when(ctx.decode(String.class, textType)).thenReturn("decoded string");

    InputStreamBody body = new InputStreamBody(ctx, new ByteArrayInputStream(new byte[0]), 0L);

    assertEquals("decoded string", body.to(String.class));
    assertEquals("decoded string", body.toNullable(String.class));
  }
}
