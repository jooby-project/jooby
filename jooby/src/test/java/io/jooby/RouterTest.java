package io.jooby;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RouterTest {
  @Test
  public void pathKeys() {
    pathKeys("/{lang:[a-z]{2}}", keys -> {
      assertEquals(Collections.singletonList("lang"), keys);
    });

    pathKeys("/*", keys -> assertEquals(1, keys.size()));

    pathKeys("/foo/?*", keys -> assertEquals(1, keys.size()));

    pathKeys("/foo", keys -> assertEquals(0, keys.size()));
    pathKeys("/", keys -> assertEquals(0, keys.size()));
    pathKeys("/foo/bar", keys -> assertEquals(0, keys.size()));
    pathKeys("/foo/*", keys -> assertEquals(1, keys.size()));
    pathKeys("/foo/*name", keys -> assertEquals(1, keys.size()));
    pathKeys("/foo/{x}", keys -> assertEquals(1, keys.size()));
  }

  @Test
  public void reverseByPosition() {
    assertEquals("/foo", Router.reverse("/{k}", "foo"));
    assertEquals("/foo/bar", Router.reverse("/{k1}/{k2}", "foo", "bar"));
  }

  @Test
  public void reverse() {
    assertEquals("foo", Router.reverse("{foo}", map("foo", "foo")));

    assertEquals("/tail", Router.reverse("/*", map("*", "tail")));

    assertEquals("/file/video.mpg", Router.reverse("/file/*", map("*", "video.mpg")));
    assertEquals("/file/video.mpg", Router.reverse("/file/*video", map("video", "video.mpg")));

    assertEquals("/123", Router.reverse("/{regex:\\d+}", map("regex", 123)));

    assertEquals("/resources/123/edit", Router.reverse("/resources/{num:\\d+}/edit", map("num", 123)));

    assertEquals("/prefix/v1/v2", Router.reverse("/prefix/{k1}/{k2}", map("k1", "v1", "k2", "v2")));

    assertEquals("/v1/suffix", Router.reverse("/{k1}/suffix", map("k1", "v1", "k2", "v2")));

    assertEquals("/foo/v", Router.reverse("/{name}/{k}", map("name", "foo", "k", "v")));

    assertEquals("/foo", Router.reverse("/{name}", map("name", "foo")));

    assertEquals("/", Router.reverse("/", Collections.emptyMap()));
    assertEquals("/path", Router.reverse("/path", Collections.emptyMap()));
    assertEquals("/path", Router.reverse("/path", map("k", "v")));

  }

  private void pathKeys(String pattern, Consumer<List<String>> consumer) {
    consumer.accept(Router.pathKeys(pattern));
  }

  public Map<String, Object> map(Object... values) {
    Map<String, Object> map = new HashMap<>();
    for (int i = 0; i < values.length; i += 2) {
      map.put(values[i].toString(), values[i + 1]);
    }
    return map;
  }
}
