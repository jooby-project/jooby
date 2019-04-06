package io.jooby;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiConsumer;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class EnvironmentTest {

  @Test
  public void defaultEnv() {

    env("foo", (env, conf) -> {
      assertEquals("[dev]\n"
          + "└── system properties\n"
          + " └── env variables\n"
          + "  └── classpath://env/foo/application.conf\n"
          + "   └── defaults", env.toString());
      assertEquals("[dev]", env.getActiveNames().toString());
      assertEquals(System.getProperty("user.dir"), conf.getString("user.dir"));
      assertEquals("bar", conf.getString("foo"));
      assertEquals(asList("a", "b", "c"), conf.getStringList("letters"));
    });

    env("foo", mapOf("application.env", "PROD"), (env, conf) -> {
      assertEquals("[prod]\n"
          + "└── system properties\n"
          + " └── env variables\n"
          + "  └── classpath://env/foo/application.prod.conf\n"
          + "   └── classpath://env/foo/application.conf\n"
          + "    └── defaults", env.toString());
      assertEquals("[prod]", env.getActiveNames().toString());
      assertEquals("bazz", conf.getString("foo"));
      assertEquals(asList("a", "b", "c"), conf.getStringList("letters"));
    });

    env("foo", mapOf("application.env", "Test, bar"), (env, conf) -> {
      assertEquals("[test, bar]\n"
          + "└── system properties\n"
          + " └── env variables\n"
          + "  └── classpath://env/foo/application.test.conf\n"
          + "   └── classpath://env/foo/application.bar.conf\n"
          + "    └── defaults", env.toString());
      assertEquals("[test, bar]", env.getActiveNames().toString());
      assertEquals("test", conf.getString("foo"));
      assertEquals(asList("d"), conf.getStringList("letters"));
    });

    env("empty", (env, conf) -> {
      assertEquals("[dev]", env.getActiveNames().toString());
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

    Environment env = Environment
        .loadEnvironment(new EnvironmentOptions().setBasedir(basedir).setActiveNames("prod"));
    assertEquals("bazz", env.getConfig().getString("foo"));
    assertEquals("[prod]\n"
        + "└── system properties\n"
        + " └── env variables\n"
        + "  └── src/test/resources/env/foo/application.prod.conf\n"
        + "   └── src/test/resources/env/foo/application.conf\n"
        + "    └── defaults", env.toString());
  }

  @Test
  public void objectLookup() {

    Environment env = new Environment(ConfigFactory.parseMap(mapOf("h.pool", "1", "h.db.pool", "2")), "test");

    assertEquals("1", env.getConfig().getString("h.pool"));
    assertEquals("2", env.getConfig().getString("h.db.pool"));
    assertEquals(mapOf("pool", "2"), env.getConfig().getConfig("h.db").root().unwrapped());
  }

  @Test
  public void args() {
    Map<String, String> args = Jooby.parseArguments("foo", " bar = ");
    assertEquals("{application.env=foo, bar=}", args.toString());

    assertEquals(Collections.emptyMap(), Jooby.parseArguments());
    assertEquals(Collections.emptyMap(), Jooby.parseArguments((String[]) null));
  }

  private void env(String dir, BiConsumer<Environment, Config> consumer) {
    env(dir, Collections.emptyMap(), consumer);
  }

  private void env(String dir, Map<String, String> args, BiConsumer<Environment, Config> consumer) {
    Properties sysprops = new Properties();
    sysprops.putAll(System.getProperties());
    try {
      String[] names = args.getOrDefault("application.env", "dev").split(",");
      args.forEach((k, v) -> System.setProperty(k, v));
      Environment env = Environment.loadEnvironment(new EnvironmentOptions()
          .setBasedir("env/" + dir)
          .setActiveNames(names)
      );
      consumer.accept(env, env.getConfig());
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
