/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class EnvironmentTest {

  @Test
  public void defaultEnv() {

    env(
        "foo",
        (env, conf) -> {
          assertEnvMatches(
              env,
              "[dev]"::equals,
              "system properties"::equals,
              "env variables"::equals,
              pathEndsMatch("env/foo/application.conf"),
              "defaults"::equals);
          assertEquals("[dev]", env.getActiveNames().toString());
          assertEquals(System.getProperty("user.dir"), conf.getString("user.dir"));
          assertEquals("bar", conf.getString("foo"));
          assertEquals(asList("a", "b", "c"), conf.getStringList("letters"));
        });

    env(
        "foo",
        mapOf("application.env", "PROD"),
        (env, conf) -> {
          assertEnvMatches(
              env,
              "[prod]"::equals,
              "system properties"::equals,
              "env variables"::equals,
              pathEndsMatch("env/foo/application.prod.conf"),
              pathEndsMatch("env/foo/application.conf"),
              "defaults"::equals);
          assertEquals("[prod]", env.getActiveNames().toString());
          assertEquals("bazz", conf.getString("foo"));
          assertEquals(asList("a", "b", "c"), conf.getStringList("letters"));
        });

    env(
        "foo",
        mapOf("application.env", "Test, bar"),
        (env, conf) -> {
          assertEnvMatches(
              env,
              "[test, bar]"::equals,
              "system properties"::equals,
              "env variables"::equals,
              pathEndsMatch("env/foo/application.test.conf"),
              pathEndsMatch("env/foo/application.bar.conf"),
              "defaults"::equals);
          assertEquals("[test, bar]", env.getActiveNames().toString());
          assertEquals("test", conf.getString("foo"));
          assertEquals(asList("d"), conf.getStringList("letters"));
        });

    env(
        "empty",
        (env, conf) -> {
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
    Path reldir = Paths.get("src/test/resources/env/foo");
    basedir =
        basedir.resolve("src").resolve("test").resolve("resources").resolve("env").resolve("foo");

    Environment env =
        Environment.loadEnvironment(
            new EnvironmentOptions().setBasedir(basedir.toString()).setActiveNames("prod"));
    assertEquals("bazz", env.getConfig().getString("foo"));
    assertEnvMatches(
        env,
        "[prod]"::equals,
        "system properties"::equals,
        "env variables"::equals,
        pathEndsMatch(reldir.resolve("application.prod.conf").toString()),
        pathEndsMatch(reldir.resolve("application.conf").toString()),
        "defaults"::equals);
  }

  @Test
  public void objectLookup() {

    Environment env =
        new Environment(
            getClass().getClassLoader(),
            ConfigFactory.parseMap(mapOf("h.pool", "1", "h.db.pool", "2")),
            "test");

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

  @Test
  public void flattenProperties() {
    Config config =
        ConfigFactory.parseMap(
            mapOf(
                "k",
                "v",
                "root",
                mapOf("map", mapOf("key1", 1, "key2", "2", "list", Arrays.asList("a", "b")))));
    Environment environment = new Environment(getClass().getClassLoader(), config);
    assertEquals(
        mapOf("root.map.key1", "1", "root.map.key2", "2", "root.map.list", "a, b"),
        environment.getProperties("root"));

    assertEquals(
        mapOf("p.map.key1", "1", "p.map.key2", "2", "p.map.list", "a, b"),
        environment.getProperties("root", "p"));
  }

  // --- Start of newly added tests for 100% coverage ---

  @Test
  public void testConstructorsAndAccessors() {
    Config conf = ConfigFactory.empty();
    Environment env = new Environment(getClass().getClassLoader(), conf, "dev", "test");
    assertEquals(Arrays.asList("dev", "test"), env.getActiveNames());
    assertEquals(getClass().getClassLoader(), env.getClassLoader());

    Config newConf = ConfigFactory.parseMap(Map.of("k", "v"));
    env.setConfig(newConf);
    assertEquals(newConf, env.getConfig());
  }

  @Test
  public void testGetProperty() {
    Config conf = ConfigFactory.parseMap(Map.of("foo", "bar"));
    Environment env = new Environment(getClass().getClassLoader(), conf, "dev");

    assertEquals("bar", env.getProperty("foo"));
    assertNull(env.getProperty("missing"));

    assertEquals("bar", env.getProperty("foo", "default"));
    assertEquals("default", env.getProperty("missing", "default"));
  }

  @Test
  public void testGetPropertiesEdgeCases() {
    Config conf = ConfigFactory.parseMap(Map.of("foo", Map.of("a", "b")));
    Environment env = new Environment(getClass().getClassLoader(), conf, "dev");

    // Key doesn't exist
    assertTrue(env.getProperties("missing").isEmpty());

    // Null/Empty prefix
    assertEquals(Map.of("a", "b"), env.getProperties("foo", null));
    assertEquals(Map.of("a", "b"), env.getProperties("foo", ""));
  }

  @Test
  public void testIsActive() {
    Environment env =
        new Environment(getClass().getClassLoader(), ConfigFactory.empty(), "prod", "QA");
    assertTrue(env.isActive("prod"));
    assertTrue(env.isActive("qa"));
    assertTrue(env.isActive("dev", "PROD"));
    assertFalse(env.isActive("dev", "test"));
  }

  @Test
  public void testLoadClass() {
    Environment env = new Environment(getClass().getClassLoader(), ConfigFactory.empty(), "dev");
    assertTrue(env.loadClass("java.lang.String").isPresent());
    assertFalse(env.loadClass("com.example.DoesNotExist").isPresent());
  }

  @Test
  public void testPidWithSystemProperty() {
    String original = System.getProperty("PID");
    try {
      System.setProperty("PID", "12345");
      assertEquals("12345", Environment.pid());
    } finally {
      if (original != null) {
        System.setProperty("PID", original);
      } else {
        System.clearProperty("PID");
      }
    }
  }

  @Test
  public void loadEnvironment_LocalEnvFallback() throws Exception {
    Path tempDir = Files.createTempDirectory("jooby-env-test");
    tempDir.toFile().deleteOnExit();
    Path appConf = tempDir.resolve("application.conf");
    Path localConf = tempDir.resolve("application.localdev.conf");

    Files.write(appConf, "application.env = localdev\nfallback.key = appconf".getBytes());
    Files.write(localConf, "fallback.key = localdevconf".getBytes());

    EnvironmentOptions options =
        new EnvironmentOptions().setBasedir(tempDir.toString()).setActiveNames("dev");

    Environment env = Environment.loadEnvironment(options);

    assertEquals(Collections.singletonList("localdev"), env.getActiveNames());
    assertEquals("localdevconf", env.getConfig().getString("fallback.key"));
  }

  @Test
  public void loadEnvironment_NoExtension() throws Exception {
    Path tempDir = Files.createTempDirectory("jooby-env-test2");
    tempDir.toFile().deleteOnExit();
    Path customFile = tempDir.resolve("myconfig.conf");
    Files.write(customFile, "custom.key = true".getBytes());

    EnvironmentOptions options =
        new EnvironmentOptions().setBasedir(tempDir.toString()).setFilename("myconfig");

    Environment env = Environment.loadEnvironment(options);
    assertTrue(env.getConfig().getBoolean("custom.key"));
  }

  @Test
  public void classpathConfigWithBasedir() {
    // Uses src/test/resources/env/foo which is on the classpath.
    // basedir="env/foo" will not be found as a directory by fileConfig relative to cwd in most
    // setups, so it correctly falls back to classpathConfig which hits our target branches.
    EnvironmentOptions options =
        new EnvironmentOptions().setBasedir("env/foo").setActiveNames("prod");

    Environment env = Environment.loadEnvironment(options);
    assertEquals("bazz", env.getConfig().getString("foo"));
  }

  @Test
  public void testSystemPropertiesAndEnv() {
    Config props = Environment.systemProperties();
    System.out.println(props.getAnyRef("java.version"));
    assertEquals(System.getProperty("java.version"), props.getString("java.version"));

    Config env = Environment.systemEnv();
    assertNotNull(env);
  }

  @Test
  public void testHasPathException() {
    Environment env = new Environment(getClass().getClassLoader(), ConfigFactory.empty());
    // Passing a single quote `"` forces a ConfigException (Parse error) hitting the catch block
    assertNull(env.getProperty("\""));
    assertEquals("default", env.getProperty("\"", "default"));
    assertTrue(env.getProperties("\"").isEmpty());
  }

  // --- End of newly added tests ---

  private void env(String dir, BiConsumer<Environment, Config> consumer) {
    env(dir, Collections.emptyMap(), consumer);
  }

  private void env(String dir, Map<String, Object> args, BiConsumer<Environment, Config> consumer) {
    Properties sysprops = new Properties();
    sysprops.putAll(System.getProperties());
    try {
      String[] names = args.getOrDefault("application.env", "dev").toString().split(",");
      args.forEach((k, v) -> System.setProperty(k, v.toString()));
      Environment env =
          Environment.loadEnvironment(
              new EnvironmentOptions().setBasedir("env/" + dir).setActiveNames(names));
      consumer.accept(env, env.getConfig());
    } finally {
      System.setProperties(sysprops);
    }
  }

  private Map<String, Object> mapOf(Object... values) {
    Map<String, Object> hash = new HashMap<>();
    for (int i = 0; i < values.length; i += 2) {
      hash.put(values[i].toString(), values[i + 1]);
    }
    return hash;
  }

  private static Predicate<String> pathEndsMatch(String path) {
    return line -> line.replaceAll("\\\\", "/").endsWith(path.replaceAll("\\\\", "/"));
  }

  @SafeVarargs
  private static void assertEnvMatches(Environment environment, Predicate<String>... predicates) {
    final String[] lines = environment.toString().split(">|;");
    assertEquals(lines.length, predicates.length);
    for (int i = 0; i < predicates.length; ++i) {
      assertTrue(
          predicates[i].test(lines[i].trim()),
          "Predicate for line nr. " + i + " should succeed." + predicates[i] + " != " + lines[i]);
    }
  }
}
