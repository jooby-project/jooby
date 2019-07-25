package io.jooby;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RouterTest {
  @Test
  public void pathKeys() {
    pathKeys("/*", keys -> assertEquals(1, keys.size()));

    pathKeys("/foo/?*", keys -> assertEquals(1, keys.size()));

    pathKeys("/foo", keys -> assertEquals(0, keys.size()));
    pathKeys("/", keys -> assertEquals(0, keys.size()));
    pathKeys("/foo/bar", keys -> assertEquals(0, keys.size()));
    pathKeys("/foo/*", keys -> assertEquals(1, keys.size()));
    pathKeys("/foo/{x}", keys -> assertEquals(1, keys.size()));
  }

  private void pathKeys(String pattern, Consumer<List<String>> consumer) {
    consumer.accept(Router.pathKeys(pattern));
  }
}
