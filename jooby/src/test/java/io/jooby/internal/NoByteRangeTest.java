/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jooby.ByteRange;
import io.jooby.Context;
import io.jooby.StatusCode;

public class NoByteRangeTest {

  @Test
  @DisplayName("Verify default values for full content length")
  void testProperties() {
    long length = 1024L;
    NoByteRange range = new NoByteRange(length);

    assertEquals(0, range.getStart());
    assertEquals(length, range.getEnd());
    assertEquals(length, range.getContentLength());
    assertEquals(StatusCode.OK, range.getStatusCode());
    assertEquals("bytes */1024", range.getContentRange());
  }

  @Test
  @DisplayName("Verify identity application to Context and InputStream")
  void testApply() throws IOException {
    NoByteRange range = new NoByteRange(500L);
    Context ctx = mock(Context.class);

    // apply(Context) should return the same instance
    ByteRange resultRange = range.apply(ctx);
    assertSame(range, resultRange);

    // apply(InputStream) should return the same input stream
    InputStream input = new ByteArrayInputStream(new byte[0]);
    InputStream resultStream = range.apply(input);
    assertSame(input, resultStream);
  }
}
