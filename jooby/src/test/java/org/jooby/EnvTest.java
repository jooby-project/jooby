package org.jooby;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Key;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import org.jooby.funzy.Throwing;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.Test;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;

public class EnvTest {

  @Test
  public void resolveOneAtMiddle() {
    Config config = ConfigFactory.empty()
        .withValue("contextPath", ConfigValueFactory.fromAnyRef("/myapp"));

    Env env = Env.DEFAULT.build(config);
    assertEquals("function ($) {$.ajax(\"/myapp/api\")}",
        env.resolve("function ($) {$.ajax(\"${contextPath}/api\")}"));
  }

  @Test
  public void resolveSingle() {
    Config config = ConfigFactory.empty()
        .withValue("var", ConfigValueFactory.fromAnyRef("foo.bar"));

    Env env = Env.DEFAULT.build(config);
    assertEquals("foo.bar", env.resolve("${var}"));
  }

  @Test
  public void altplaceholder() {

    Env env = Env.DEFAULT.build(ConfigFactory.empty()
        .withValue("var", ConfigValueFactory.fromAnyRef("foo.bar")));

    assertEquals("foo.bar", env.resolver().delimiters("{{", "}}").resolve("{{var}}"));
    assertEquals("foo.bar", env.resolver().delimiters("<%", "%>").resolve("<%var%>"));
  }

  @Test
  public void resolveHead() {
    Config config = ConfigFactory.empty()
        .withValue("var", ConfigValueFactory.fromAnyRef("foo.bar"));

    Env env = Env.DEFAULT.build(config);
    assertEquals("foo.bar-", env.resolve("${var}-"));
  }

  @Test
  public void resolveTail() {
    Config config = ConfigFactory.empty()
        .withValue("var", ConfigValueFactory.fromAnyRef("foo.bar"));

    Env env = Env.DEFAULT.build(config);
    assertEquals("-foo.bar", env.resolve("-${var}"));
  }

  @Test
  public void resolveMore() {
    Config config = ConfigFactory.empty()
        .withValue("var", ConfigValueFactory.fromAnyRef("foo.bar"));

    Env env = Env.DEFAULT.build(config);
    assertEquals("foo.bar - foo.bar", env.resolve("${var} - ${var}"));
  }

  @Test
  public void resolveMap() {
    Config config = ConfigFactory.empty();

    Env env = Env.DEFAULT.build(config);
    assertEquals("foo.bar - foo.bar", env.resolver().source(ImmutableMap.of("var", "foo.bar"))
        .resolve("${var} - ${var}"));
  }

  @Test
  public void resolveMapIgnore() {
    Config config = ConfigFactory.empty();

    Env env = Env.DEFAULT.build(config);
    assertEquals("${varx} - ${varx}",
        env.resolver().ignoreMissing().source(ImmutableMap.of("var", "foo.bar"))
            .resolve("${varx} - ${varx}"));
  }

  @Test
  public void resolveIgnoreMissing() {
    Config config = ConfigFactory.empty();

    Env env = Env.DEFAULT.build(config);
    assertEquals("${var} - ${var}", env.resolver().ignoreMissing().resolve("${var} - ${var}"));

    assertEquals(" - ${foo.var} -", env.resolver().ignoreMissing().resolve(" - ${foo.var} -"));
  }

  @Test
  public void novars() {
    Config config = ConfigFactory.empty()
        .withValue("var", ConfigValueFactory.fromAnyRef("foo.bar"));

    Env env = Env.DEFAULT.build(config);
    assertEquals("var", env.resolve("var"));
  }

  @Test
  public void globalObject() {
    Config config = ConfigFactory.empty()
        .withValue("var", ConfigValueFactory.fromAnyRef("foo.bar"));

    Env env = Env.DEFAULT.build(config);
    Object value = new Object();
    env.set(Object.class, value);
    assertEquals(value, env.get(Object.class).get());
  }

  @Test
  public void serviceKey() {
    Config config = ConfigFactory.empty()
        .withValue("var", ConfigValueFactory.fromAnyRef("foo.bar"));

    Env env = Env.DEFAULT.build(config);
    assertNotNull(env.serviceKey());

    assertNotNull(new Env() {
      @Override public <T> Env set(Key<T> key, T value) {
        throw new UnsupportedOperationException();
      }

      @Override public <T> Optional<T> get(Key<T> key) {
        throw new UnsupportedOperationException();
      }

      @Nullable @Override public <T> T unset(Key<T> key) {
        throw new UnsupportedOperationException();
      }

      @Override
      public LifeCycle onStart(final Throwing.Consumer<Registry> task) {
        return null;
      }

      @Override
      public LifeCycle onStarted(final Throwing.Consumer<Registry> task) {
        return null;
      }

      @Override
      public LifeCycle onStop(final Throwing.Consumer<Registry> task) {
        return null;
      }

      @Override
      public Map<String, Function<String, String>> xss() {
        return null;
      }

      @Override
      public Env xss(final String name, final Function<String, String> escaper) {
        return null;
      }

      @Override
      public String name() {
        return null;
      }

      @Override
      public Router router() throws UnsupportedOperationException {
        return null;
      }

      @Override
      public Config config() {
        return null;
      }

      @Override
      public Locale locale() {
        return null;
      }

      @Override
      public List<Throwing.Consumer<Registry>> startTasks() {
        return null;
      }

      @Override
      public List<Throwing.Consumer<Registry>> startedTasks() {
        return null;
      }

      @Override
      public List<Throwing.Consumer<Registry>> stopTasks() {
        return null;
      }

    }.serviceKey());
  }

  @Test(expected = NullPointerException.class)
  public void nullText() {
    Env env = Env.DEFAULT.build(ConfigFactory.empty());
    env.resolve(null);
  }

  @Test
  public void unclosedDelimiterWithSpace() {
    Env env = Env.DEFAULT.build(ConfigFactory.empty());
    try {
      env.resolve(env.resolve("function ($) {$.ajax(\"${contextPath /api\")"));
      fail();
    } catch (IllegalArgumentException ex) {
      assertEquals("found '${' expecting '}' at 1:23", ex.getMessage());
    }
  }

  @Test
  public void unclosedDelimiter() {
    Env env = Env.DEFAULT.build(ConfigFactory.empty());
    try {
      env.resolve(env.resolve("function ($) {$.ajax(\"${contextPath/api\")"));
      fail();
    } catch (IllegalArgumentException ex) {
      assertEquals("found '${' expecting '}' at 1:23", ex.getMessage());
    }
  }

  @Test
  public void noSuchKey() {
    Env env = Env.DEFAULT.build(ConfigFactory.empty());
    try {
      env.resolve(env.resolve("${key}"));
      fail();
    } catch (NoSuchElementException ex) {
      assertEquals("Missing ${key} at 1:1", ex.getMessage());
    }

    try {
      env.resolve(env.resolve("    ${key}"));
      fail();
    } catch (NoSuchElementException ex) {
      assertEquals("Missing ${key} at 1:5", ex.getMessage());
    }

    try {
      env.resolve(env.resolve("  \n  ${key}"));
      fail();
    } catch (NoSuchElementException ex) {
      assertEquals("Missing ${key} at 2:3", ex.getMessage());
    }

    try {
      env.resolve(env.resolve("  \n  ${key}"));
      fail();
    } catch (NoSuchElementException ex) {
      assertEquals("Missing ${key} at 2:3", ex.getMessage());
    }

    try {
      env.resolve(env.resolve("  \n \n ${key}"));
      fail();
    } catch (NoSuchElementException ex) {
      assertEquals("Missing ${key} at 3:2", ex.getMessage());
    }
  }

  @Test
  public void resolveEmpty() {
    Env env = Env.DEFAULT.build(ConfigFactory.empty());
    assertEquals("", env.resolve(""));
  }

  @Test
  public void ifMode() throws Throwable {
    assertEquals("$dev",
        Env.DEFAULT.build(ConfigFactory.empty()).ifMode("dev", () -> "$dev").get());
    assertEquals(Optional.empty(),
        Env.DEFAULT.build(ConfigFactory.empty()).ifMode("prod", () -> "$dev"));

    assertEquals(
        "$prod",
        Env.DEFAULT
            .build(
                ConfigFactory.empty().withValue("application.env",
                    ConfigValueFactory.fromAnyRef("prod")))
            .ifMode("prod", () -> "$prod").get());
    assertEquals(Optional.empty(),
        Env.DEFAULT
            .build(
                ConfigFactory.empty().withValue("application.env",
                    ConfigValueFactory.fromAnyRef("prod")))
            .ifMode("dev", () -> "$prod"));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void noRouter() {
    Env.DEFAULT.build(ConfigFactory.empty()).router();
  }

  @Test
  public void name() throws Exception {
    assertEquals("dev", Env.DEFAULT.build(ConfigFactory.empty()).toString());

    assertEquals("prod", Env.DEFAULT.build(ConfigFactory.empty().withValue("application.env",
        ConfigValueFactory.fromAnyRef("prod"))).toString());

  }

  @Test
  public void onStart() throws Exception {
    Env env = Env.DEFAULT.build(ConfigFactory.empty());
    Throwing.Runnable task = () -> {
    };
    env.onStart(task);

    assertEquals(1, env.startTasks().size());
  }

  @Test
  public void onStop() throws Exception {
    Env env = Env.DEFAULT.build(ConfigFactory.empty());
    Throwing.Runnable task = () -> {
    };
    env.onStop(task);

    assertEquals(1, env.stopTasks().size());
  }
}
