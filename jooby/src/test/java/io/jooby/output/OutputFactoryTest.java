/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.output;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class OutputFactoryTest {

  private OutputFactory factory;
  private OutputOptions options;

  @BeforeEach
  void setUp() {
    // Mock the interface but tell Mockito to call the real 'default' method implementations
    factory = mock(OutputFactory.class, CALLS_REAL_METHODS);

    // Set up a deterministic OutputOptions for the default allocate() methods to pull from
    options = new OutputOptions().setSize(1024).setDirectBuffers(true);
  }

  @Test
  @DisplayName(
      "Verify create(OutputOptions) returns a ByteBufferedOutputFactory with the provided options")
  void testCreateWithOptions() {
    OutputOptions customOptions = new OutputOptions();
    OutputFactory newFactory = OutputFactory.create(customOptions);

    assertNotNull(newFactory);
    assertEquals("ByteBufferedOutputFactory", newFactory.getClass().getSimpleName());
    assertSame(customOptions, newFactory.getOptions());
  }

  @Test
  @DisplayName("Verify create() returns a ByteBufferedOutputFactory with default options")
  void testCreateDefault() {
    OutputFactory newFactory = OutputFactory.create();

    assertNotNull(newFactory);
    assertEquals("ByteBufferedOutputFactory", newFactory.getClass().getSimpleName());
    assertNotNull(newFactory.getOptions());
  }

  @Test
  @DisplayName("Verify allocate(size) delegates to allocate(direct, size) using options")
  void testAllocateWithSize() {
    when(factory.getOptions()).thenReturn(options);

    factory.allocate(2048);

    // It should pull isDirectBuffers (true) from the options and pass the 2048 size
    verify(factory).allocate(true, 2048);
  }

  @Test
  @DisplayName("Verify allocate() delegates to allocate(direct, size) using options")
  void testAllocateDefault() {
    when(factory.getOptions()).thenReturn(options);

    factory.allocate();

    // It should pull both isDirectBuffers (true) and size (1024) from the options
    verify(factory).allocate(true, 1024);
  }

  @Test
  @DisplayName("Verify wrap(String) delegates to wrap(String, UTF_8) and then wrap(byte[])")
  void testWrapString() {
    factory.wrap("hello");

    // The default method wrap(String) converts to bytes using UTF-8 and calls wrap(byte[])
    verify(factory).wrap("hello".getBytes(StandardCharsets.UTF_8));
  }

  @Test
  @DisplayName("Verify wrap(String, Charset) delegates to wrap(byte[]) with the correct Charset")
  void testWrapStringWithCharset() {
    factory.wrap("hello", StandardCharsets.UTF_16);

    // The default method wrap(String, Charset) converts using the provided charset
    verify(factory).wrap("hello".getBytes(StandardCharsets.UTF_16));
  }
}
