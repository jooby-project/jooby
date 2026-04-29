/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.pebble;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.junit.jupiter.api.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.jooby.Environment;
import io.jooby.Jooby;
import io.jooby.ModelAndView;
import io.jooby.ServiceRegistry;
import io.jooby.output.Output;
import io.jooby.test.MockContext;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.cache.CacheKey;
import io.pebbletemplates.pebble.cache.PebbleCache;
import io.pebbletemplates.pebble.loader.Loader;
import io.pebbletemplates.pebble.template.PebbleTemplate;

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
    var output =
        engine.render(
            ctx,
            ModelAndView.map("index.peb").put("user", new User("foo", "bar")).put("sign", "!"));
    assertEquals(
        "Hello foo bar var!", StandardCharsets.UTF_8.decode(output.asByteBuffer()).toString());
  }

  @Test
  public void shouldUnsupportedModelAndView() {
    PebbleEngine.Builder builder =
        PebbleModule.create()
            .build(new Environment(getClass().getClassLoader(), ConfigFactory.empty()));
    PebbleTemplateEngine engine =
        new PebbleTemplateEngine(builder, Collections.singletonList(".peb"));
    MockContext ctx = new MockContext();
    assertThrows(
        ModelAndView.UnsupportedModelAndView.class,
        () -> engine.render(ctx, new ModelAndView<>("index.peb", Map.of("foo", "bar"))));
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
    var output =
        engine.render(
            ctx,
            ModelAndView.map("index.peb").put("user", new User("foo", "bar")).put("sign", "!"));
    assertEquals(
        "Hello foo bar var!", StandardCharsets.UTF_8.decode(output.asByteBuffer()).toString());
  }

  @Test
  public void renderWithLocale() throws Exception {
    PebbleEngine.Builder builder =
        PebbleModule.create()
            .build(new Environment(getClass().getClassLoader(), ConfigFactory.empty()));
    var engine = new PebbleTemplateEngine(builder, List.of(".peb"));
    MockContext ctx = new MockContext().setRouter(new Jooby().setLocales(List.of(Locale.ENGLISH)));

    assertEquals("Greetings!", toString(engine.render(ctx, ModelAndView.map("locales.peb"))));

    assertEquals(
        "Hi!",
        toString(
            engine.render(ctx, ModelAndView.map("locales.peb").setLocale(new Locale("en", "GB")))));

    assertEquals(
        "Grüß Gott!",
        toString(engine.render(ctx, ModelAndView.map("locales.peb").setLocale(Locale.GERMAN))));

    assertEquals(
        "Grüß Gott!",
        toString(engine.render(ctx, ModelAndView.map("locales.peb").setLocale(Locale.GERMANY))));

    assertEquals(
        "Servus!",
        toString(
            engine.render(ctx, ModelAndView.map("locales.peb").setLocale(new Locale("de", "AT")))));
  }

  // --- Branch and Line Coverage Tests ---

  @Test
  public void installDefault() throws Exception {
    Jooby app = mock(Jooby.class);
    Environment env = mock(Environment.class);
    Config config = mock(Config.class);
    ServiceRegistry registry = mock(ServiceRegistry.class);

    when(app.getEnvironment()).thenReturn(env);
    when(app.getServices()).thenReturn(registry);
    when(env.getConfig()).thenReturn(config);
    when(env.isActive("dev", "test")).thenReturn(false);

    PebbleModule module = new PebbleModule();
    module.install(app);

    verify(app).encoder(any(PebbleTemplateEngine.class));
    verify(registry).put(eq(PebbleEngine.Builder.class), any(PebbleEngine.Builder.class));
  }

  @Test
  public void installCustomBuilderConstructor() throws Exception {
    Jooby app = mock(Jooby.class);
    ServiceRegistry registry = mock(ServiceRegistry.class);
    when(app.getServices()).thenReturn(registry);

    PebbleEngine.Builder engineBuilder = new PebbleEngine.Builder();
    PebbleModule module = new PebbleModule(engineBuilder);

    module.install(app);

    verify(app).encoder(any(PebbleTemplateEngine.class));
    verify(registry).put(PebbleEngine.Builder.class, engineBuilder);
  }

  @Test
  public void buildWithConfigOptions() {
    Environment env = mock(Environment.class);
    Config config = mock(Config.class);
    when(env.getConfig()).thenReturn(config);
    when(env.isActive("dev", "test")).thenReturn(false);

    // Mock all branches to true
    when(config.hasPath("pebble.cacheActive")).thenReturn(true);
    when(config.getBoolean("pebble.cacheActive")).thenReturn(true);

    when(config.hasPath("pebble.strictVariables")).thenReturn(true);
    when(config.getBoolean("pebble.strictVariables")).thenReturn(true);

    when(config.hasPath("pebble.allowUnsafeMethods")).thenReturn(true);
    when(config.getBoolean("pebble.allowUnsafeMethods")).thenReturn(true);

    when(config.hasPath("pebble.literalDecimalTreatedAsInteger")).thenReturn(true);
    when(config.getBoolean("pebble.literalDecimalTreatedAsInteger")).thenReturn(true);

    when(config.hasPath("pebble.greedyMatchMethod")).thenReturn(true);
    when(config.getBoolean("pebble.greedyMatchMethod")).thenReturn(true);

    when(config.hasPath("pebble.extension")).thenReturn(true);
    when(config.getString("pebble.extension")).thenReturn(".html");

    PebbleModule.Builder builder = PebbleModule.create();
    PebbleEngine.Builder engineBuilder = builder.build(env);

    assertNotNull(engineBuilder);
  }

  @Test
  public void buildWithSafeMethods() {
    Environment env = mock(Environment.class);
    Config config = mock(Config.class);
    when(env.getConfig()).thenReturn(config);
    when(env.isActive("dev", "test")).thenReturn(false);

    when(config.hasPath("pebble.allowUnsafeMethods")).thenReturn(true);
    // Explicitly test the false branch for unsafe methods (BlacklistMethodAccessValidator)
    when(config.getBoolean("pebble.allowUnsafeMethods")).thenReturn(false);

    PebbleModule.Builder builder = PebbleModule.create();
    assertNotNull(builder.build(env));
  }

  @Test
  public void buildDevEnv() {
    Environment env = mock(Environment.class);
    Config config = mock(Config.class);
    when(env.getConfig()).thenReturn(config);
    // Trigger the active dev/test env block
    when(env.isActive("dev", "test")).thenReturn(true);

    PebbleModule.Builder builder = PebbleModule.create();
    assertNotNull(builder.build(env));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void customBuilderProperties() {
    Environment env = mock(Environment.class);
    Config config = mock(Config.class);
    when(env.getConfig()).thenReturn(config);
    when(env.isActive("dev", "test")).thenReturn(false);

    PebbleCache<CacheKey, Object> tagCache = mock(PebbleCache.class);
    PebbleCache<Object, PebbleTemplate> templateCache = mock(PebbleCache.class);
    ExecutorService executor = mock(ExecutorService.class);
    Loader<?> loader = mock(Loader.class);

    PebbleModule.Builder builder =
        PebbleModule.create()
            .setTagCache(tagCache)
            .setTemplateCache(templateCache)
            .setExecutorService(executor)
            .setDefaultLocale(Locale.CANADA)
            .setTemplateLoader(loader);

    assertNotNull(builder.build(env));
  }

  @Test
  public void loaderWithLeadingSlashPath() {
    Environment env = mock(Environment.class);
    Config config = mock(Config.class);
    when(env.getConfig()).thenReturn(config);
    when(env.isActive("dev", "test")).thenReturn(false);

    // Forces coverage into the if (value.startsWith("/")) strip block
    PebbleModule.Builder builder = PebbleModule.create().setTemplatesPath("/custom-views");

    assertNotNull(builder.build(env));
  }

  @Test
  public void loaderWithNullPathForcesFallbackToTemplateEnginePath() {
    Environment env = mock(Environment.class);
    Config config = mock(Config.class);
    when(env.getConfig()).thenReturn(config);
    when(env.isActive("dev", "test")).thenReturn(false);

    // Forces coverage of if (templatesPath == null) templatesPath = TemplateEngine.PATH;
    PebbleModule.Builder builder = PebbleModule.create().setTemplatesPath(null);

    assertNotNull(builder.build(env));
  }

  private String toString(Output output) {
    return StandardCharsets.UTF_8.decode(output.asByteBuffer()).toString();
  }
}
