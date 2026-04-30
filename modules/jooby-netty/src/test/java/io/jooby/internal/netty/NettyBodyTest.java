/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.jooby.Context;
import io.jooby.MediaType;
import io.jooby.value.Value;
import io.jooby.value.ValueFactory;
import io.netty.handler.codec.http.multipart.HttpData;

@ExtendWith(MockitoExtension.class)
class NettyBodyTest {

  @Mock Context ctx;
  @Mock HttpData data;

  @Test
  void testIsInMemory() {
    when(data.isInMemory()).thenReturn(true);
    NettyBody body = new NettyBody(ctx, data, 100L);
    assertTrue(body.isInMemory());

    when(data.isInMemory()).thenReturn(false);
    assertFalse(body.isInMemory());
  }

  @Test
  void testGetSize() {
    NettyBody body = new NettyBody(ctx, data, 1024L);
    assertEquals(1024L, body.getSize());
  }

  @Test
  void testStream_InMemory() throws IOException {
    when(data.isInMemory()).thenReturn(true);
    when(data.get()).thenReturn(new byte[] {1, 2, 3});

    NettyBody body = new NettyBody(ctx, data, 3L);
    InputStream stream = body.stream();

    assertTrue(stream instanceof ByteArrayInputStream);
    assertEquals(1, stream.read());
  }

  @Test
  void testStream_File() throws IOException {
    File tempFile = File.createTempFile("netty-body-test", ".tmp");
    tempFile.deleteOnExit();

    when(data.isInMemory()).thenReturn(false);
    when(data.getFile()).thenReturn(tempFile);

    NettyBody body = new NettyBody(ctx, data, 0L);
    InputStream stream = body.stream();

    assertTrue(stream instanceof FileInputStream);
    stream.close();
  }

  @Test
  void testStream_IOException() throws IOException {
    when(data.isInMemory()).thenReturn(true);
    when(data.get()).thenThrow(new IOException("Forced Error"));

    NettyBody body = new NettyBody(ctx, data, 0L);

    // SneakyThrows wraps it in a RuntimeException
    assertThrows(IOException.class, body::stream);
  }

  @Test
  void testGetAndGetOrDefault() {
    ValueFactory factory = mock(ValueFactory.class);
    when(ctx.getValueFactory()).thenReturn(factory);

    NettyBody body = new NettyBody(ctx, data, 0L);

    // test get()
    Value missingValue = body.get("missingKey");
    assertTrue(missingValue.isMissing());
    assertEquals("missingKey", missingValue.name());

    // test getOrDefault()
    Value defaultValue = body.getOrDefault("key", "fallback");
    assertEquals("fallback", defaultValue.value());
    assertEquals("key", defaultValue.name());
  }

  @Test
  void testChannel() throws IOException {
    when(data.isInMemory()).thenReturn(true);
    when(data.get()).thenReturn(new byte[] {1, 2, 3});

    NettyBody body = new NettyBody(ctx, data, 3L);
    ReadableByteChannel channel = body.channel();

    assertNotNull(channel);
    assertTrue(channel.isOpen());
    channel.close();
  }

  @Test
  void testBytes_InMemory() throws IOException {
    byte[] expected = {4, 5, 6};
    when(data.isInMemory()).thenReturn(true);
    when(data.get()).thenReturn(expected);

    NettyBody body = new NettyBody(ctx, data, 3L);
    assertArrayEquals(expected, body.bytes());
  }

  @Test
  void testBytes_File() throws IOException {
    File tempFile = File.createTempFile("netty-body-test-bytes", ".tmp");
    tempFile.deleteOnExit();
    byte[] expected = "File Content".getBytes(StandardCharsets.UTF_8);
    Files.write(tempFile.toPath(), expected);

    when(data.isInMemory()).thenReturn(false);
    when(data.getFile()).thenReturn(tempFile);

    NettyBody body = new NettyBody(ctx, data, (long) expected.length);
    assertArrayEquals(expected, body.bytes());
  }

  @Test
  void testBytes_IOException() throws IOException {
    when(data.isInMemory()).thenReturn(true);
    when(data.get()).thenThrow(new IOException("Forced Bytes Error"));

    NettyBody body = new NettyBody(ctx, data, 0L);

    // SneakyThrows wraps it in a RuntimeException
    assertThrows(IOException.class, body::bytes);
  }

  @Test
  void testValue() throws IOException {
    String content = "Hello Jooby";
    when(data.isInMemory()).thenReturn(true);
    when(data.get()).thenReturn(content.getBytes(StandardCharsets.UTF_8));

    NettyBody body = new NettyBody(ctx, data, (long) content.length());
    assertEquals(content, body.value());
  }

  @Test
  void testName() {
    NettyBody body = new NettyBody(ctx, data, 0L);
    assertEquals("body", body.name());
  }

  @Test
  void testTo() {
    MediaType textType = MediaType.text;
    when(ctx.getRequestType(MediaType.text)).thenReturn(textType);
    when(ctx.decode(eq(String.class), eq(textType))).thenReturn("DecodedResult");

    NettyBody body = new NettyBody(ctx, data, 0L);
    String result = body.to(String.class);

    assertEquals("DecodedResult", result);
    verify(ctx).decode(String.class, textType);
  }

  @Test
  void testToNullable() {
    MediaType textType = MediaType.text;
    when(ctx.getRequestType(MediaType.text)).thenReturn(textType);
    when(ctx.decode(eq(String.class), eq(textType))).thenReturn(null);

    NettyBody body = new NettyBody(ctx, data, 0L);
    String result = body.toNullable(String.class);

    assertEquals(null, result);
    verify(ctx).decode(String.class, textType);
  }

  @Test
  void testToMultimap() {
    NettyBody body = new NettyBody(ctx, data, 0L);
    Map<String, java.util.List<String>> map = body.toMultimap();

    assertNotNull(map);
    assertTrue(map.isEmpty());
  }
}
