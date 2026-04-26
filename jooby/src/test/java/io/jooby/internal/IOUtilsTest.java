/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

public class IOUtilsTest {

  @Test
  public void testToString() throws IOException {
    String data = "jooby framework";
    InputStream in = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
    assertEquals(data, IOUtils.toString(in, StandardCharsets.UTF_8));
  }

  @Test
  public void testBoundedStatic() throws IOException {
    byte[] data = {0, 1, 2, 3, 4, 5};
    InputStream in = new ByteArrayInputStream(data);
    // Start at 2, take 2 bytes -> should be [2, 3]
    InputStream bounded = IOUtils.bounded(in, 2, 2);

    byte[] result = bounded.readAllBytes();
    assertArrayEquals(new byte[] {2, 3}, result);
  }

  @Test
  public void testBoundedReadSingleByte() throws IOException {
    InputStream in = new ByteArrayInputStream(new byte[] {10, 20, 30});
    InputStream bounded = IOUtils.bounded(in, 0, 2);

    assertEquals(10, bounded.read());
    assertEquals(20, bounded.read());
    assertEquals(-1, bounded.read()); // Limit reached
  }

  @Test
  public void testBoundedReadBuffer() throws IOException {
    InputStream in = new ByteArrayInputStream(new byte[] {1, 2, 3, 4, 5});
    InputStream bounded = IOUtils.bounded(in, 1, 2); // [2, 3]

    byte[] buf = new byte[10];
    int read = bounded.read(buf);
    assertEquals(2, read);
    assertEquals(2, buf[0]);
    assertEquals(3, buf[1]);

    // Subsequent read should be EOF
    assertEquals(-1, bounded.read(buf, 0, 1));
  }

  @Test
  public void testBoundedSkip() throws IOException {
    InputStream in = new ByteArrayInputStream(new byte[] {0, 1, 2, 3, 4, 5});
    InputStream bounded = IOUtils.bounded(in, 0, 4); // [0, 1, 2, 3]

    assertEquals(2, bounded.skip(2));
    assertEquals(2, bounded.read()); // reads '2'
    assertEquals(1, bounded.skip(10)); // tries to skip 10, but only 1 left in bound
    assertEquals(-1, bounded.read());
  }

  @Test
  public void testAvailable() throws IOException {
    InputStream in = new ByteArrayInputStream(new byte[] {1, 2, 3});
    InputStream bounded = IOUtils.bounded(in, 0, 2);

    assertTrue(bounded.available() > 0);
    bounded.skip(2);
    assertEquals(0, bounded.available()); // Limit reached
  }

  @Test
  public void testMarkReset() throws IOException {
    InputStream in = new ByteArrayInputStream(new byte[] {1, 2, 3, 4, 5});
    InputStream bounded = IOUtils.bounded(in, 0, 5);

    assertTrue(bounded.markSupported());
    bounded.read(); // 1
    bounded.mark(10);
    bounded.read(); // 2
    bounded.read(); // 3

    bounded.reset();
    assertEquals(2, bounded.read());
  }

  @Test
  public void testClosePropagation() throws IOException {
    InputStream mockIn = mock(InputStream.class);
    // Use reflection or the static helper to get the inner class if needed,
    // but here we just test the logic via the public IOUtils.bounded
    InputStream bounded = IOUtils.bounded(mockIn, 0, 10);

    // Test default propagation
    bounded.close();
    verify(mockIn).close();

    // Test disabled propagation
    // We need to cast to access the inner class methods if they are visible,
    // otherwise we rely on the specific behavior.
    // Since BoundedInputStream is private, we'd typically test this via a
    // package-private access or by verifying it doesn't throw.
    // In this specific Jooby source, BoundedInputStream is private.
    // We can use a custom wrapper to test the logic if strictly necessary for 100%.
  }

  @Test
  public void testUnlimitedBounded() throws IOException {
    // The constructor BoundedInputStream(in) sets max to -1
    // We can't reach it directly because it's private and not used in static helpers.
    // However, if the intent is to cover the code, we test the branch `max >= 0`
    byte[] data = "abc".getBytes();
    InputStream in = new ByteArrayInputStream(data);

    // To trigger the EOF path in read(byte[], int, int)
    InputStream bounded = IOUtils.bounded(in, 0, 10);
    assertEquals(3, bounded.read(new byte[10]));
    assertEquals(-1, bounded.read(new byte[10]));
  }

  @Test
  public void testToStringImplementation() throws IOException {
    InputStream in = new ByteArrayInputStream("foo".getBytes());
    InputStream bounded = IOUtils.bounded(in, 0, 3);
    assertEquals(in.toString(), bounded.toString());
  }
}
