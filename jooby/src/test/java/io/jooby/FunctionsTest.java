package io.jooby;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FunctionsTest {

  @Test
  public void closer() {
    AtomicInteger success = new AtomicInteger();

    IllegalStateException x = assertThrows(IllegalStateException.class, () -> {

      try (Functions.Closer closer = Functions.closer()) {
        closer.register(() -> {
          throw new IllegalStateException("First error");
        });

        closer.register(() ->
            success.incrementAndGet()
        );

        closer.register(() -> {
          throw new IOException("Second error");
        });

        closer.register(() ->
            success.incrementAndGet()
        );
      }
    });

    assertEquals("First error", x.getMessage());
    assertEquals(2, success.get());
  }
}
