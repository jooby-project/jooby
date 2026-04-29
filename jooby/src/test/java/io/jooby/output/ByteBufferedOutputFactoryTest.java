/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.output;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ByteBufferedOutputFactoryTest {

  private OutputOptions options;
  private ByteBufferedOutputFactory factory;

  @BeforeEach
  void setUp() {
    options = new OutputOptions();
    factory = new ByteBufferedOutputFactory(options);
  }

  @Test
  @DisplayName("Verify getOptions returns the supplied OutputOptions")
  void testGetOptions() {
    assertSame(options, factory.getOptions());
  }

  @Test
  @DisplayName("Verify allocate returns a new ByteBufferedOutput")
  void testAllocate() {
    BufferedOutput output = factory.allocate(false, 1024);
    assertEquals(ByteBufferedOutput.class, output.getClass());
  }

  @Test
  @DisplayName("Verify newComposite returns an internal CompositeOutput")
  void testNewComposite() {
    BufferedOutput composite = factory.newComposite();
    assertEquals("CompositeOutput", composite.getClass().getSimpleName());
  }

  @Test
  @DisplayName("Verify default factory wrap methods return internal OutputStatic instances")
  void testDefaultFactoryWrapMethods() {
    // wrap(ByteBuffer)
    Output wrappedBuf = factory.wrap(ByteBuffer.allocate(10));
    assertEquals("OutputStatic", wrappedBuf.getClass().getSimpleName());

    // wrap(String, Charset) -> delegates to wrap(byte[])
    Output wrappedStr = factory.wrap("hello", StandardCharsets.UTF_8);
    assertEquals("OutputStatic", wrappedStr.getClass().getSimpleName());

    // wrap(byte[]) -> delegates to wrap(byte[], offset, length)
    Output wrappedBytes = factory.wrap(new byte[] {1, 2, 3});
    assertEquals("OutputStatic", wrappedBytes.getClass().getSimpleName());

    // wrap(byte[], offset, length)
    Output wrappedOffset = factory.wrap(new byte[] {1, 2, 3, 4}, 1, 2);
    assertEquals("OutputStatic", wrappedOffset.getClass().getSimpleName());
  }

  @Test
  @DisplayName("Verify ContextOutputFactory wrap methods return internal WrappedOutput instances")
  void testContextFactoryWrapMethods() {
    OutputFactory ctxFactory = factory.getContextFactory();

    // Verify it created the inner class correctly
    assertEquals("ContextOutputFactory", ctxFactory.getClass().getSimpleName());

    // Verify inheritance of options
    assertSame(options, ctxFactory.getOptions());

    // wrap(ByteBuffer)
    Output wrappedBuf = ctxFactory.wrap(ByteBuffer.allocate(10));
    assertEquals("WrappedOutput", wrappedBuf.getClass().getSimpleName());

    // wrap(String, Charset)
    Output wrappedStr = ctxFactory.wrap("hello", StandardCharsets.UTF_8);
    assertEquals("WrappedOutput", wrappedStr.getClass().getSimpleName());

    // wrap(byte[])
    Output wrappedBytes = ctxFactory.wrap(new byte[] {1, 2, 3});
    assertEquals("WrappedOutput", wrappedBytes.getClass().getSimpleName());

    // wrap(byte[], offset, length)
    Output wrappedOffset = ctxFactory.wrap(new byte[] {1, 2, 3, 4}, 1, 2);
    assertEquals("WrappedOutput", wrappedOffset.getClass().getSimpleName());
  }
}
