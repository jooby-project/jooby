package org.jooby;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Optional;

import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

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

    assertEquals("foo.bar", env.resolve("{{var}}", "{{", "}}"));
    assertEquals("foo.bar", env.resolve("<%var%>", "<%", "%>"));
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
  public void novars() {
    Config config = ConfigFactory.empty()
        .withValue("var", ConfigValueFactory.fromAnyRef("foo.bar"));

    Env env = Env.DEFAULT.build(config);
    assertEquals("var", env.resolve("var"));
  }

  @Test(expected = NullPointerException.class)
  public void nullText() {
    Env env = Env.DEFAULT.build(ConfigFactory.empty());
    env.resolve(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyStartDelim() {
    Env env = Env.DEFAULT.build(ConfigFactory.empty());
    env.resolve("{{var}}", "", "}");
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyEndDelim() {
    Env env = Env.DEFAULT.build(ConfigFactory.empty());
    env.resolve("{{var}}", "${", "");
  }

  @Test
  public void unclosedDelimiterWithSpace() {
    Env env = Env.DEFAULT.build(ConfigFactory.empty());
    try {
      env.resolve(env.resolve("function ($) {$.ajax(\"${contextPath /api\")"));
      fail();
    } catch (IllegalArgumentException ex) {
      assertEquals("Unclosed placeholder: ${contextPath", ex.getMessage());
    }
  }

  @Test
  public void unclosedDelimiter() {
    Env env = Env.DEFAULT.build(ConfigFactory.empty());
    try {
      env.resolve(env.resolve("function ($) {$.ajax(\"${contextPath/api\")"));
      fail();
    } catch (IllegalArgumentException ex) {
      assertEquals("Unclosed placeholder: ${contextPath/api\")", ex.getMessage());
    }
  }

  @Test
  public void resolveEmpty() {
    Env env = Env.DEFAULT.build(ConfigFactory.empty());
    assertEquals("", env.resolve(""));
  }

  @Test
  public void ifMode() throws Throwable {
    assertEquals("$dev", Env.DEFAULT.build(ConfigFactory.empty()).ifMode("dev", () -> "$dev").get());
    assertEquals(Optional.empty(),
        Env.DEFAULT.build(ConfigFactory.empty()).ifMode("prod", () -> "$dev"));

    assertEquals(
        "$prod",
        Env.DEFAULT
            .build(
                ConfigFactory.empty().withValue("application.env",
                    ConfigValueFactory.fromAnyRef("prod"))).ifMode("prod", () -> "$prod").get());
    assertEquals(Optional.empty(),
        Env.DEFAULT
            .build(
                ConfigFactory.empty().withValue("application.env",
                    ConfigValueFactory.fromAnyRef("prod"))).ifMode("dev", () -> "$prod"));
  }

  @Test
  public void when() throws Throwable {
    assertEquals("$dev", Env.DEFAULT.build(ConfigFactory.empty()).when("dev", () -> "$dev").get());

    assertEquals("$dev", Env.DEFAULT.build(ConfigFactory.empty()).when("dev", "$dev").get());

    assertEquals("$dev",
        Env.DEFAULT.build(ConfigFactory.empty()).when((env) -> env.equals("dev"), "$dev").get());
  }

  @Test
  public void name() throws Exception {
    assertEquals("dev", Env.DEFAULT.build(ConfigFactory.empty()).toString());

    assertEquals("prod", Env.DEFAULT.build(ConfigFactory.empty().withValue("application.env",
        ConfigValueFactory.fromAnyRef("prod"))).toString());

  }
}
