package io.jooby;

import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ByteRangeTest {

  @Test
  public void noByteRange() {
    range("bytes=-", range -> {
      assertEquals(ByteRange.NOT_SATISFIABLE, range);
      assertEquals(false, range.valid());
    });
    range(null, range -> {
      assertEquals(ByteRange.NO_RANGE, range);
      assertEquals(false, range.valid());
    });
    range("foo", range -> {
      assertEquals(ByteRange.NOT_SATISFIABLE, range);
      assertEquals(false, range.valid());
    });

    range("bytes=", range -> {
      assertEquals(ByteRange.NOT_SATISFIABLE, range);
      assertEquals(false, range.valid());
    });
    range("bytes=z-", range -> {
      assertEquals(ByteRange.NOT_SATISFIABLE, range);
      assertEquals(false, range.valid());
    });
    range("bytes=-z", range -> {
      assertEquals(ByteRange.NOT_SATISFIABLE, range);
      assertEquals(false, range.valid());
    });
    range("bytes=6", range -> {
      assertEquals(ByteRange.NOT_SATISFIABLE, range);
      assertEquals(false, range.valid());
    });
  }

  @Test
  public void byteRange() {
    range("bytes=1-10", range -> {
      assertEquals(true, range.valid());
      assertEquals(1, range.start);
      assertEquals(10, range.end);
    });

    range("bytes=99-", range -> {
      assertEquals(true, range.valid());
      assertEquals(99, range.start);
      assertEquals(-1, range.end);
    });

    range("bytes=-99", range -> {
      assertEquals(true, range.valid());
      assertEquals(-1, range.start);
      assertEquals(99, range.end);
    });

    // 100-150 is ignored.
    range("bytes=0-50, 100-150", range -> {
      assertEquals(true, range.valid());
      assertEquals(0, range.start);
      assertEquals(50, range.end);
    });
  }

  private void range(String value, Consumer<ByteRange> consumer) {
    consumer.accept(ByteRange.parse(value));
  }
}
