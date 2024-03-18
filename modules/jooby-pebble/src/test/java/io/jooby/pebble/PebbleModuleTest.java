/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.pebble;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import com.typesafe.config.ConfigFactory;
import io.jooby.Environment;
import io.jooby.Jooby;
import io.jooby.ModelAndView;
import io.jooby.test.MockContext;
import io.pebbletemplates.pebble.PebbleEngine;

public class PebbleModuleTest {
  public static class User {
    private String firstname;

    private String lastname;

    public User(String firstname, String lastname) {
      this.firstname = firstname;
      this.lastname = lastname;
    }

    public String getFirstname() {
      return firstname;
    }

    public String getLastname() {
      return lastname;
    }
  }

  @Test
  public void render() throws Exception {
    PebbleEngine.Builder builder =
        PebbleModule.create()
            .build(new Environment(getClass().getClassLoader(), ConfigFactory.empty()));
    PebbleTemplateEngine engine =
        new PebbleTemplateEngine(builder, Collections.singletonList(".peb"));
    MockContext ctx =
        new MockContext().setRouter(new Jooby().setLocales(singletonList(Locale.ENGLISH)));
    ctx.getAttributes().put("local", "var");
    String output =
        engine.render(
            ctx,
            ModelAndView.map("index.peb").put("user", new User("foo", "bar")).put("sign", "!"));
    assertEquals("Hello foo bar var!", output);
  }

  @Test
  public void renderFileSystem() throws Exception {
    PebbleEngine.Builder builder =
        PebbleModule.create()
            .setTemplatesPath(Paths.get("src", "test", "resources", "views").toString())
            .build(new Environment(getClass().getClassLoader(), ConfigFactory.empty()));
    PebbleTemplateEngine engine =
        new PebbleTemplateEngine(builder, Collections.singletonList(".peb"));
    MockContext ctx =
        new MockContext().setRouter(new Jooby().setLocales(singletonList(Locale.ENGLISH)));
    ctx.getAttributes().put("local", "var");
    String output =
        engine.render(
            ctx,
            ModelAndView.map("index.peb").put("user", new User("foo", "bar")).put("sign", "!"));
    assertEquals("Hello foo bar var!", output);
  }

  @Test
  public void renderWithLocale() throws Exception {
    PebbleEngine.Builder builder =
        PebbleModule.create()
            .build(new Environment(getClass().getClassLoader(), ConfigFactory.empty()));
    PebbleTemplateEngine engine =
        new PebbleTemplateEngine(builder, Collections.singletonList(".peb"));
    MockContext ctx =
        new MockContext().setRouter(new Jooby().setLocales(singletonList(Locale.ENGLISH)));

    assertEquals("Greetings!", engine.render(ctx, ModelAndView.map("locales.peb")));

    assertEquals(
        "Hi!",
        engine.render(ctx, ModelAndView.map("locales.peb").setLocale(new Locale("en", "GB"))));

    assertEquals(
        "Grüß Gott!", engine.render(ctx, ModelAndView.map("locales.peb").setLocale(Locale.GERMAN)));

    assertEquals(
        "Grüß Gott!",
        engine.render(ctx, ModelAndView.map("locales.peb").setLocale(Locale.GERMANY)));

    assertEquals(
        "Servus!",
        engine.render(ctx, ModelAndView.map("locales.peb").setLocale(new Locale("de", "AT"))));
  }
}
