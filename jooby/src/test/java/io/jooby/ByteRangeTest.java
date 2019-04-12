package io.jooby;

import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static io.jooby.StatusCode.PARTIAL_CONTENT;
import static io.jooby.StatusCode.REQUESTED_RANGE_NOT_SATISFIABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ByteRangeTest {

  @Test
  public void noByteRange() {
    range("bytes=-", 10, range -> {
      assertEquals(REQUESTED_RANGE_NOT_SATISFIABLE, range.getStatusCode());
    });
    range(null, 10, range -> {
      assertEquals(StatusCode.OK, range.getStatusCode());
    });
    range("foo", 100, range -> {
      assertEquals(REQUESTED_RANGE_NOT_SATISFIABLE, range.getStatusCode());
    });

    range("bytes=", 10, range -> {
      assertEquals(REQUESTED_RANGE_NOT_SATISFIABLE, range.getStatusCode());
    });
    range("bytes=z-", 10, range -> {
      assertEquals(REQUESTED_RANGE_NOT_SATISFIABLE, range.getStatusCode());
    });
    range("bytes=-z", 10, range -> {
      assertEquals(REQUESTED_RANGE_NOT_SATISFIABLE, range.getStatusCode());
    });
  }

  @Test
  public void byteRange() {
    range("bytes=1-10", 10, range -> {
      assertEquals(PARTIAL_CONTENT, range.getStatusCode());
      assertEquals(1, range.getStart());
      assertEquals(9, range.getEnd());
      assertEquals(9, range.getContentLength());
      assertEquals("bytes 1-9/10", range.getContentRange());
    });

    range("bytes=99-", 110, range -> {
      assertEquals(PARTIAL_CONTENT, range.getStatusCode());
      assertEquals(99, range.getStart());
      assertEquals(11, range.getEnd());
      assertEquals("bytes 99-109/110", range.getContentRange());
    });

    range("bytes=-99", 200, range -> {
      assertEquals(PARTIAL_CONTENT, range.getStatusCode());
      assertEquals(101, range.getStart());
      assertEquals(99, range.getEnd());
      assertEquals("bytes 101-199/200", range.getContentRange());
    });

    // 100-150 is ignored.
    range("bytes=0-50, 100-150", 200, range -> {
      assertEquals(PARTIAL_CONTENT, range.getStatusCode());
      assertEquals(0, range.getStart());
      assertEquals(51, range.getEnd());
      assertEquals("bytes 0-50/200", range.getContentRange());
    });
  }

  private void range(String value, long len, Consumer<ByteRange> consumer) {
    consumer.accept(ByteRange.parse(value, len));
  }
}
