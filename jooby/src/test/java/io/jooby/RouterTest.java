package io.jooby;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RouterTest {
  @Test
  public void pathKeys() {
    pathKeys("/{lang:[a-z]{2}}", keys -> {
      assertEquals(Collections.singletonList("lang"), keys);
    });

    pathKeys("/edit/{id}?", keys -> {
      assertEquals(Collections.singletonList("id"), keys);
    });

    pathKeys("/path/{id}/{start}?/{end}?", keys -> {
      assertEquals(Arrays.asList("id", "start", "end"), keys);
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
  public void pathKeyMap() {
    pathKeyMap("/{lang:[a-z]{2}}", map -> {
      assertEquals("[a-z]{2}", map.get("lang"));
    });

    pathKeyMap("/{id:[0-9]+}", map -> {
      assertEquals("[0-9]+", map.get("id"));
    });

    pathKeyMap("/edit/{id}?", keys -> {
      assertEquals(null, keys.get("id"));
    });

    pathKeyMap("/path/{id}/{start}?/{end}?", keys -> {
      assertEquals(null, keys.get("id"));
      assertEquals(null, keys.get("start"));
      assertEquals(null, keys.get("end"));
    });

    pathKeyMap("/*", keys -> {
      assertEquals("\\.*", keys.get("*"));
    });

    pathKeyMap("/foo/?*", keys -> {
      assertEquals("\\.*", keys.get("*"));
    });

    pathKeyMap("/foo/*name", keys -> {
      assertEquals("\\.*", keys.get("name"));
    });
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

    assertEquals("/resources/123/edit",
        Router.reverse("/resources/{num:\\d+}/edit", map("num", 123)));

    assertEquals("/prefix/v1/v2", Router.reverse("/prefix/{k1}/{k2}", map("k1", "v1", "k2", "v2")));

    assertEquals("/v1/suffix", Router.reverse("/{k1}/suffix", map("k1", "v1", "k2", "v2")));

    assertEquals("/foo/v", Router.reverse("/{name}/{k}", map("name", "foo", "k", "v")));

    assertEquals("/foo", Router.reverse("/{name}", map("name", "foo")));

    assertEquals("/", Router.reverse("/", Collections.emptyMap()));
    assertEquals("/path", Router.reverse("/path", Collections.emptyMap()));
    assertEquals("/path", Router.reverse("/path", map("k", "v")));
  }

  @Test
  public void shouldExpandOptionalParams() {
    parse("/{lang:[a-z]{2}}?", paths -> {
      assertEquals(2, paths.size());
      assertEquals("/", paths.get(0));
      assertEquals("/{lang:[a-z]{2}}", paths.get(1));
    });
    parse("/{lang:[a-z]{2}}", paths -> {
      assertEquals(1, paths.size());
      assertEquals("/{lang:[a-z]{2}}", paths.get(0));
    });
    parse("/edit/{id:[0-9]+}?", paths -> {
      assertEquals(2, paths.size());
      assertEquals("/edit", paths.get(0));
      assertEquals("/edit/{id:[0-9]+}", paths.get(1));
    });
    parse("/path/{id}/{start}?/{end}?", paths -> {
      assertEquals(3, paths.size());
      assertEquals("/path/{id}", paths.get(0));
      assertEquals("/path/{id}/{start}", paths.get(1));
      assertEquals("/path/{id}/{start}/{end}", paths.get(2));
    });
    parse("/{id}?/suffix", paths -> {
      assertEquals(3, paths.size());
      assertEquals("/", paths.get(0));
      assertEquals("/{id}/suffix", paths.get(1));
      assertEquals("/suffix", paths.get(2));
    });
    parse("/prefix/{id}?", paths -> {
      assertEquals(2, paths.size());
      assertEquals("/prefix", paths.get(0));
      assertEquals("/prefix/{id}", paths.get(1));
    });
    parse("/{id}?", paths -> {
      assertEquals(2, paths.size());
      assertEquals("/", paths.get(0));
      assertEquals("/{id}", paths.get(1));
    });
    parse("/path", paths -> {
      assertEquals(1, paths.size());
      assertEquals("/path", paths.get(0));
    });

    parse("/path/subpath", paths -> {
      assertEquals(1, paths.size());
      assertEquals("/path/subpath", paths.get(0));
    });

    parse("/{id}", paths -> {
      assertEquals(1, paths.size());
      assertEquals("/{id}", paths.get(0));
    });

    parse("/{id}/suffix", paths -> {
      assertEquals(1, paths.size());
      assertEquals("/{id}/suffix", paths.get(0));
    });

    parse("/prefix/{id}", paths -> {
      assertEquals(1, paths.size());
      assertEquals("/prefix/{id}", paths.get(0));
    });

    parse("/", paths -> {
      assertEquals(1, paths.size());
      assertEquals("/", paths.get(0));
    });

    parse(null, paths -> {
      assertEquals(1, paths.size());
      assertEquals("/", paths.get(0));
    });

    parse("", paths -> {
      assertEquals(1, paths.size());
      assertEquals("/", paths.get(0));
    });
  }

  private void pathKeys(String pattern, Consumer<List<String>> consumer) {
    consumer.accept(Router.pathKeys(pattern));
  }

  private void pathKeyMap(String pattern, Consumer<Map<String, String>> consumer) {
    Map<String, String> map = new HashMap<>();
    Router.pathKeys(pattern, map::put);
    consumer.accept(map);
  }

  private void parse(String pattern, Consumer<List<String>> consumer) {
    consumer.accept(Router.expandOptionalVariables(pattern));
  }

  public Map<String, Object> map(Object... values) {
    Map<String, Object> map = new HashMap<>();
    for (int i = 0; i < values.length; i += 2) {
      map.put(values[i].toString(), values[i + 1]);
    }
    return map;
  }
}
