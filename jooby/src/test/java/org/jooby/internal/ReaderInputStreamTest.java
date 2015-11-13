package org.jooby.internal;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;

import org.junit.Test;

public class ReaderInputStreamTest {

  @Test
  public void empty() throws IOException {
    try (ReaderInputStream reader = new ReaderInputStream(new StringReader(""), UTF_8)) {
      assertEquals(-1, reader.read());
    }

  }

  @Test
  public void one() throws IOException {
    try (ReaderInputStream reader = new ReaderInputStream(new StringReader("a"), UTF_8)) {
      assertEquals(97, reader.read());
    }
  }

  @Test
  public void read0() throws IOException {
    try (ReaderInputStream reader = new ReaderInputStream(new StringReader("a"), UTF_8)) {
      assertEquals(0, reader.read(new byte[0]));
    }
  }

}
