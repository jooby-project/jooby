/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import io.jooby.Context;
import io.jooby.StatusCode;
import io.jooby.exception.StatusCodeException;

public class NotSatisfiableByteRangeTest {

  @Test
  public void testGettersAndMetadata() {
    long length = 1024L;
    String rangeValue = "bytes=1000-2000";
    NotSatisfiableByteRange range = new NotSatisfiableByteRange(rangeValue, length);

    assertEquals(-1, range.getStart());
    assertEquals(-1, range.getEnd());
    assertEquals(length, range.getContentLength());
    assertEquals(StatusCode.REQUESTED_RANGE_NOT_SATISFIABLE, range.getStatusCode());
    assertEquals("bytes */1024", range.getContentRange());
  }

  @Test
  public void testApplyContext() {
    NotSatisfiableByteRange range = new NotSatisfiableByteRange("invalid", 100);
    Context ctx = mock(Context.class);

    StatusCodeException ex = assertThrows(StatusCodeException.class, () -> range.apply(ctx));
    assertEquals(StatusCode.REQUESTED_RANGE_NOT_SATISFIABLE, ex.getStatusCode());
  }

  @Test
  public void testApplyInputStream() {
    NotSatisfiableByteRange range = new NotSatisfiableByteRange("invalid", 100);
    InputStream is = new ByteArrayInputStream(new byte[0]);

    StatusCodeException ex = assertThrows(StatusCodeException.class, () -> range.apply(is));
    assertEquals(StatusCode.REQUESTED_RANGE_NOT_SATISFIABLE, ex.getStatusCode());
  }
}
