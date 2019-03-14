package io.jooby;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class EnvTest {

  @Test
  public void defaultEnv() {

    env("foo", (env, conf) -> {
      assertEquals("dev\n"
          + "└── system.properties\n"
          + " └── env variables\n"
          + "  └── classpath://env/foo/application.conf\n"
          + "   └── defaults", env.toString());
      assertEquals("dev", env.name());
      assertEquals(System.getProperty("user.dir"), conf.getString("user.dir"));
      assertEquals("bar", conf.getString("foo"));
      assertEquals(asList("a", "b", "c"), conf.getStringList("letters"));
    });

    env("foo", mapOf("application.env", "PROD"), (env, conf) -> {
      assertEquals("prod\n"
          + "└── system.properties\n"
          + " └── env variables\n"
          + "  └── classpath://env/foo/application.prod.conf\n"
          + "   └── classpath://env/foo/application.conf\n"
          + "    └── defaults", env.toString());
      assertEquals("prod", env.name());
      assertEquals("bazz", conf.getString("foo"));
      assertEquals(asList("a", "b", "c"), conf.getStringList("letters"));
    });

    env("empty", (env, conf) -> {
      assertEquals("dev", env.name());
    });
  }

  @Test
  public void envfromfs() {
    Path basedir = Paths.get(System.getProperty("user.dir"));
    if (Files.exists(basedir.resolve("jooby"))) {
      // maven vs IDE
      basedir = basedir.resolve("jooby");
    }
    basedir = basedir.resolve("src").resolve("test").resolve("resources").resolve("env")
        .resolve("foo");

    Env env = Env.create()
        .basedir(basedir)
        .build(getClass().getClassLoader(), "prod");
    assertEquals("bazz", env.conf().getString("foo"));
    assertEquals("dev\n"
        + "└── system.properties\n"
        + " └── env variables\n"
        + "  └── src/test/resources/env/foo/application.prod.conf\n"
        + "   └── src/test/resources/env/foo/application.conf\n"
        + "    └── defaults", env.toString());
  }

  @Test
  public void objectLookup() {

    Env env = new Env("test", ConfigFactory.parseMap(mapOf("h.pool", "1", "h.db.pool", "2")));

    assertEquals("1", env.conf().getString("h.pool"));
    assertEquals("2", env.conf().getString("h.db.pool"));
    assertEquals(mapOf("pool", "2"), env.conf().getConfig("h.db").root().unwrapped());
  }

  @Test
  public void args() {
    Map<String, String> args = Env.parse("foo", " bar = ");
    assertEquals("{bar=, application.env=foo}", args.toString());

    assertEquals(Collections.emptyMap(), Env.parse());
    assertEquals(Collections.emptyMap(), Env.parse((String[]) null));
  }

  private void env(String dir, BiConsumer<Env, Config> consumer) {
    env(dir, Collections.emptyMap(), consumer);
  }

  private void env(String dir, Map<String, String> args, BiConsumer<Env, Config> consumer) {
    Properties sysprops = new Properties();
    sysprops.putAll(System.getProperties());
    try {
      args.forEach((k, v) -> System.setProperty(k, v));
      Env env = Env.create()
          .basedir("env/" + dir)
          .build(getClass().getClassLoader(), args.getOrDefault("application.env", "dev"));
      consumer.accept(env, env.conf());
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
