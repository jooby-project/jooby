package io.jooby;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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

    env("foo", mapOf("application.env", "PROD"), env -> {
      assertEquals("prod", env.name());
      assertEquals("bazz", env.get("foo").value());
      assertEquals(asList("a", "b", "c"), env.get("letters").toList());
    });

    env("empty", env -> {
      assertEquals("dev", env.name());
    });
  }

  @Test
  public void objectLookup() {

    Env env = Env.build(new Env.PropertySource("test", mapOf("h.pool", "1", "h.db.pool", "2")));

    assertEquals("1", env.get("h.pool").value());
    assertEquals("2", env.get("h.db.pool").value());
    assertEquals(mapOf("db.pool", "2"), env.get("h.db").toMap());
  }

  @Test
  public void args() {
    Env.PropertySource args = Env.parse("foo", " bar = ");
    assertEquals("args", args.name());
    assertEquals(mapOf("application.env", "foo", "bar", ""), args.properties());

    assertEquals(Collections.emptyMap(), Env.parse().properties());
    assertEquals(Collections.emptyMap(), Env.parse((String[]) null).properties());
  }

  private void env(String dir, Consumer<Env> consumer) {
    env(dir, Collections.emptyMap(), consumer);
  }

  private void env(String dir, Map<String, String> args, Consumer<Env> consumer) {
    Properties sysprops = new Properties(System.getProperties());
    try {
      Path file = Paths.get("env", dir);
      System.setProperty("env.dir", file.toString());
      args.forEach((k, v) -> System.setProperty(k, v));
      consumer.accept(Env.defaultEnvironment(getClass().getClassLoader()));
    } finally {
      System.setProperties(sysprops);
    }
  }

  private Map<String, String> mapOf(String... values) {
    Map<String, String> hash = new HashMap<>();
    for (int i = 0; i < values.length; i += 2) {
      hash.put(values[i], values[i + 1]);
    }
    return hash;
  }
}
