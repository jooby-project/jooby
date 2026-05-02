/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.jooby.StatusCode;
import io.jooby.exception.StatusCodeException;

class LimitedInputStreamTest {

  @Test
  void testReadSingleByte_UnderLimit() throws IOException {
    ByteArrayInputStream in = new ByteArrayInputStream(new byte[] {10, 20});
    LimitedInputStream limitedIn = new LimitedInputStream(in, 2);

    assertEquals(10, limitedIn.read());
    assertEquals(20, limitedIn.read());
  }

  @Test
  void testReadSingleByte_ExceedsLimit() throws IOException {
    ByteArrayInputStream in = new ByteArrayInputStream(new byte[] {10, 20, 30});
    LimitedInputStream limitedIn = new LimitedInputStream(in, 2);

    assertEquals(10, limitedIn.read()); // count = 1
    assertEquals(20, limitedIn.read()); // count = 2

    // The 3rd read pushes count to 3, exceeding the limit of 2
    StatusCodeException ex = assertThrows(StatusCodeException.class, limitedIn::read);
    assertEquals(StatusCode.REQUEST_ENTITY_TOO_LARGE, ex.getStatusCode());
  }

  @Test
  void testReadSingleByte_EOF() throws IOException {
    ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
    LimitedInputStream limitedIn = new LimitedInputStream(in, 2);

    // Reading from an empty stream returns -1 and does not increment the counter
    assertEquals(-1, limitedIn.read());
  }

  @Test
  void testReadArray_UnderLimit() throws IOException {
    ByteArrayInputStream in = new ByteArrayInputStream(new byte[] {1, 2, 3, 4, 5});
    LimitedInputStream limitedIn = new LimitedInputStream(in, 10);

    byte[] buffer = new byte[3];

    assertEquals(3, limitedIn.read(buffer, 0, 3)); // count = 3
    assertEquals(2, limitedIn.read(buffer, 0, 3)); // count = 5 (stream exhausted early)
  }

  @Test
  void testReadArray_ExceedsLimit() throws IOException {
    ByteArrayInputStream in = new ByteArrayInputStream(new byte[] {1, 2, 3, 4, 5});
    LimitedInputStream limitedIn = new LimitedInputStream(in, 4);

    byte[] buffer = new byte[3];

    // First read grabs 3 bytes. Max is 4. This is allowed.
    assertEquals(3, limitedIn.read(buffer, 0, 3));

    // Second read grabs the remaining 2 bytes. Total count hits 5. Max is 4. Throws exception.
    StatusCodeException ex =
        assertThrows(StatusCodeException.class, () -> limitedIn.read(buffer, 0, 3));
    assertEquals(StatusCode.REQUEST_ENTITY_TOO_LARGE, ex.getStatusCode());
  }

  @Test
  void testReadArray_EOF() throws IOException {
    ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
    LimitedInputStream limitedIn = new LimitedInputStream(in, 10);

    byte[] buffer = new byte[3];

    // Reading from an empty stream returns -1 and does not increment the counter
    assertEquals(-1, limitedIn.read(buffer, 0, 3));
  }

  @Test
  void testReadArray_ZeroBytesRequested() throws IOException {
    ByteArrayInputStream in = new ByteArrayInputStream(new byte[] {1, 2, 3});
    LimitedInputStream limitedIn = new LimitedInputStream(in, 2);

    byte[] buffer = new byte[3];

    // Requesting 0 bytes from the stream returns 0 immediately.
    // It should bypass the count increment logic.
    assertEquals(0, limitedIn.read(buffer, 0, 0));
  }
}
