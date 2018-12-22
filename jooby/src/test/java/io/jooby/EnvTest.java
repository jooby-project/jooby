package io.jooby;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class EnvTest {

  @Test
  public void defaultEnv() {

    env("foo", env -> {
      assertEquals("dev", env.name());
      assertEquals(System.getProperty("user.dir"), env.get("user.dir").value());
      assertEquals("bar", env.get("foo").value());
      assertEquals(asList("a", "b", "c"), env.get("letters").toList());
    });

    env("foo", Map.of("application.env", "PROD"), env -> {
      assertEquals("prod", env.name());
      assertEquals("bazz", env.get("foo").value());
      assertEquals(asList("a", "b", "c"), env.get("letters").toList());
    });

    env("empty", env -> {
      assertEquals("dev", env.name());
    });
  }

  @Test
  public void args() {
    Map<String, String> args = Env.parse("foo", " bar = ");
    assertEquals(Map.of("application.env", "foo", "bar", ""), args);

    assertEquals(Collections.emptyMap(), Env.parse());
    assertEquals(Collections.emptyMap(), Env.parse(null));
  }

  private void env(String dir, Consumer<Env> consumer) {
    env(dir, Collections.emptyMap(), consumer);
  }

  private void env(String dir, Map<String, String> args, Consumer<Env> consumer) {
    Path file = Paths.get("env", dir);
    System.setProperty("env.dir", file.toString());
    consumer.accept(Env.defaultEnvironment(toArray(args)));
  }

  private String[] toArray(Map<String, String> args) {
    List<String> result = new ArrayList<>();
    args.forEach((k, v) -> result.add(k + "=" + v));
    return result.toArray(new String[result.size()]);
  }
}
