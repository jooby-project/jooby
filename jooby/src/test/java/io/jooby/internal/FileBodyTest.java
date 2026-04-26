/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jooby.Context;
import io.jooby.MediaType;
import io.jooby.value.Value;
import io.jooby.value.ValueFactory;

public class FileBodyTest {

  private Context ctx;
  private Path tempFile;
  private FileBody body;
  private String content = "jooby file body";

  @BeforeEach
  void setUp() throws IOException {
    ctx = mock(Context.class);
    tempFile = Files.createTempFile("jooby-file-body", ".txt");
    Files.writeString(tempFile, content);
    body = new FileBody(ctx, tempFile);
  }

  @AfterEach
  void tearDown() throws IOException {
    Files.deleteIfExists(tempFile);
  }

  @Test
  void testMetadata() {
    assertEquals(content.length(), body.getSize());
    assertFalse(body.isInMemory());
    assertEquals("body", body.name());
    assertEquals(List.of(content), body.toList());
  }

  @Test
  void testContentAccess() throws IOException {
    // Bytes
    assertArrayEquals(content.getBytes(StandardCharsets.UTF_8), body.bytes());

    // Stream
    try (InputStream is = body.stream()) {
      assertEquals(content, new String(is.readAllBytes(), StandardCharsets.UTF_8));
    }

    // Channel
    try (ReadableByteChannel channel = body.channel()) {
      assertTrue(channel.isOpen());
    }

    // Value
    assertEquals(content, body.value());
  }

  @Test
  void testValueMethods() {
    ValueFactory vf = mock(ValueFactory.class);
    when(ctx.getValueFactory()).thenReturn(vf);

    // get
    Value missing = body.get("foo");
    assertTrue(missing.isMissing());
    assertEquals("foo", missing.name());

    // getOrDefault
    Value defaultValue = body.getOrDefault("bar", "def");
    assertEquals("def", defaultValue.value());
  }

  @Test
  void testTypeConversion() {
    when(ctx.getRequestType(MediaType.text)).thenReturn(MediaType.json);

    body.to(String.class);
    verify(ctx).decode(String.class, MediaType.json);

    body.toNullable(Integer.class);
    verify(ctx).decode(Integer.class, MediaType.json);

    assertTrue(body.toMultimap().isEmpty());
  }

  @Test
  void testIOExceptions() throws IOException {
    // Force file deletion to trigger IOExceptions on existing body
    Files.deleteIfExists(tempFile);

    assertThrows(NoSuchFileException.class, () -> body.getSize());
    assertThrows(NoSuchFileException.class, () -> body.bytes());
    assertThrows(NoSuchFileException.class, () -> body.stream());
    assertThrows(NoSuchFileException.class, () -> body.channel());
  }
}
