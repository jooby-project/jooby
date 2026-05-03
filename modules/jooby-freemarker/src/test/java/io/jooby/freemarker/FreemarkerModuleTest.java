/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.freemarker;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValueFactory;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.StringTemplateLoader;
import freemarker.core.XMLOutputFormat;
import freemarker.template.Configuration;
import io.jooby.*;
import io.jooby.test.MockContext;

public class FreemarkerModuleTest {

  public static class MyModel {
    public String firstname;

    public String lastname;

    public MyModel(String firstname, String lastname) {
      this.firstname = firstname;
      this.lastname = lastname;
    }
  }

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

  private Jooby app;
  private Environment env;
  private ServiceRegistry registry;
  private Config config;

  @BeforeEach
  void setUp() {
    app = mock(Jooby.class);
    env = mock(Environment.class);
    registry = mock(ServiceRegistry.class);
    config = mock(Config.class);

    when(app.getEnvironment()).thenReturn(env);
    when(app.getServices()).thenReturn(registry);
    when(env.getConfig()).thenReturn(config);
    when(config.hasPath("freemarker")).thenReturn(false);
    when(env.isActive("dev", "test")).thenReturn(false);
    when(env.getProperty(eq(TemplateEngine.TEMPLATE_PATH), anyString()))
        .thenAnswer(inv -> inv.getArgument(1)); // Return default path
    when(env.getClassLoader()).thenReturn(getClass().getClassLoader());
  }

  // --- CONSTRUCTOR & INSTALLATION TESTS ---

  @Test
  void testInstallWithExistingConfiguration() {
    Configuration customConfig = new Configuration(Configuration.VERSION_2_3_32);
    FreemarkerModule module = new FreemarkerModule(customConfig);

    module.install(app);

    verify(app).encoder(any());
    verify(registry).put(Configuration.class, customConfig);
    // Since configuration was explicitly provided, it shouldn't query the environment for a new one
    verify(env, never()).getConfig();
  }

  @Test
  void testInstallWithDefaultPath() {
    FreemarkerModule module = new FreemarkerModule();

    module.install(app);

    verify(app).encoder(any());
    verify(registry).put(eq(Configuration.class), any(Configuration.class));
  }

  @Test
  void testInstallWithStringPath() {
    FreemarkerModule module = new FreemarkerModule("custom_views");

    module.install(app);

    verify(app).encoder(any());
    verify(registry).put(eq(Configuration.class), any(Configuration.class));
  }

  @Test
  void testInstallWithNioPath(@TempDir Path tempDir) {
    FreemarkerModule module = new FreemarkerModule(tempDir);

    module.install(app);

    verify(app).encoder(any());
    verify(registry).put(eq(Configuration.class), any(Configuration.class));
  }

  // --- BUILDER TESTS ---

  @Test
  void testBuilderWithCustomTemplateLoader() {
    StringTemplateLoader stringLoader = new StringTemplateLoader();
    Configuration conf = FreemarkerModule.create().setTemplateLoader(stringLoader).build(env);

    assertEquals(stringLoader, conf.getTemplateLoader());
  }

  @Test
  void testBuilderWithSettings() {
    Configuration conf =
        FreemarkerModule.create().setSetting("tag_syntax", "square_bracket").build(env);

    assertEquals(Configuration.SQUARE_BRACKET_TAG_SYNTAX, conf.getTagSyntax());
  }

  @Test
  void testBuilderWithOutputFormat() {
    Configuration conf =
        FreemarkerModule.create().setOutputFormat(XMLOutputFormat.INSTANCE).build(env);

    assertEquals(XMLOutputFormat.INSTANCE, conf.getOutputFormat());
  }

  @Test
  void testBuilderWithConfigMap() {
    when(config.hasPath("freemarker")).thenReturn(true);

    Config freemarkerConfig = mock(Config.class);
    ConfigObject root = mock(ConfigObject.class);

    when(config.getConfig("freemarker")).thenReturn(freemarkerConfig);
    when(freemarkerConfig.root()).thenReturn(root);

    Map<String, Object> settingsMap = new HashMap<>();
    settingsMap.put("locale", "en_US");
    settingsMap.put("number_format", "0.00");
    when(root.unwrapped()).thenReturn(settingsMap);

    Configuration conf = FreemarkerModule.create().build(env);

    assertEquals(java.util.Locale.US, conf.getLocale());
    assertEquals("0.00", conf.getNumberFormat());
  }

  @Test
  void testBuilderCacheStorageInDevMode() {
    when(env.isActive("dev", "test")).thenReturn(true);

    Configuration conf = FreemarkerModule.create().build(env);

    assertEquals("freemarker.cache.NullCacheStorage", conf.getCacheStorage().getClass().getName());
  }

  @Test
  void testBuilderCacheStorageInProdMode() {
    when(env.isActive("dev", "test")).thenReturn(false);

    Configuration conf = FreemarkerModule.create().build(env);

    // prod mode defaults to soft cache
    assertEquals("freemarker.cache.MruCacheStorage", conf.getCacheStorage().getClass().getName());
  }

  // --- DEFAULT TEMPLATE LOADER RESOLUTION TESTS ---

  @Test
  void testDefaultTemplateLoaderFileSystemFallback(@TempDir Path tempDir) {
    Configuration conf = FreemarkerModule.create().setTemplatesPath(tempDir).build(env);

    // Because the temp directory exists on the file system, it should map to FileTemplateLoader
    assertTrue(conf.getTemplateLoader() instanceof FileTemplateLoader);
  }

  @Test
  void testDefaultTemplateLoaderClasspathFallback() {
    Configuration conf =
        FreemarkerModule.create()
            .setTemplatesPath("this_path_does_not_exist_on_file_system")
            .build(env);

    // Because the path doesn't exist on the file system, it should fallback to ClassTemplateLoader
    assertTrue(conf.getTemplateLoader() instanceof ClassTemplateLoader);
  }

  // --- EXCEPTION HANDLING TESTS ---

  @Test
  void testBuilderThrowsTemplateExceptionViaSneakyThrows() {
    FreemarkerModule.Builder builder =
        FreemarkerModule.create().setSetting("this_is_an_invalid_freemarker_setting_key", "value");

    // Setting an invalid freemarker config key causes setSettings to throw TemplateException
    Exception ex = assertThrows(Exception.class, () -> builder.build(env));
    assertTrue(ex instanceof freemarker.core.Configurable.UnknownSettingException);
  }

  @Test
  void testDefaultTemplateLoaderThrowsExceptionViaSneakyThrows(@TempDir Path tempDir)
      throws IOException {
    // Create a FILE, not a directory
    Path tempFile = Files.createTempFile(tempDir, "dummy", ".ftl");

    FreemarkerModule.Builder builder = FreemarkerModule.create().setTemplatesPath(tempFile);

    assertThrows(IOException.class, () -> builder.build(env));
  }

  @Test
  public void render() throws Exception {
    Configuration freemarker =
        FreemarkerModule.create()
            .build(new Environment(getClass().getClassLoader(), ConfigFactory.empty(), "test"));
    FreemarkerTemplateEngine engine =
        new FreemarkerTemplateEngine(freemarker, Arrays.asList(".ftl"));
    MockContext ctx =
        new MockContext().setRouter(new Jooby().setLocales(singletonList(Locale.ENGLISH)));
    ctx.getAttributes().put("local", "var");
    var output =
        engine.render(
            ctx,
            ModelAndView.map("index.ftl").put("user", new User("foo", "bar")).put("sign", "!"));
    assertEquals(
        "Hello foo bar var!",
        StandardCharsets.UTF_8.decode(output.asByteBuffer()).toString().trim());
  }

  @Test
  public void renderWithLocale() throws Exception {
    Configuration freemarker =
        FreemarkerModule.create()
            .build(new Environment(getClass().getClassLoader(), ConfigFactory.empty(), "test"));
    FreemarkerTemplateEngine engine =
        new FreemarkerTemplateEngine(freemarker, Arrays.asList(".ftl"));
    MockContext ctx =
        new MockContext().setRouter(new Jooby().setLocales(singletonList(Locale.ENGLISH)));

    Date nextFriday =
        java.util.Date.from(
            LocalDate.now()
                .with(TemporalAdjusters.next(DayOfWeek.FRIDAY))
                .atStartOfDay()
                .atZone(ZoneId.systemDefault())
                .toInstant());

    assertEquals(
        "friday",
        StandardCharsets.UTF_8
            .decode(
                engine
                    .render(ctx, ModelAndView.map("locales.ftl").put("someDate", nextFriday))
                    .asByteBuffer())
            .toString()
            .trim()
            .toLowerCase());

    assertEquals(
        "friday",
        StandardCharsets.UTF_8
            .decode(
                engine
                    .render(
                        ctx,
                        ModelAndView.map("locales.ftl")
                            .put("someDate", nextFriday)
                            .setLocale(new Locale("en", "GB")))
                    .asByteBuffer())
            .toString()
            .trim()
            .toLowerCase());

    assertEquals(
        "freitag",
        StandardCharsets.UTF_8
            .decode(
                engine
                    .render(
                        ctx,
                        ModelAndView.map("locales.ftl")
                            .put("someDate", nextFriday)
                            .setLocale(Locale.GERMAN))
                    .asByteBuffer())
            .toString()
            .trim()
            .toLowerCase());
  }

  @Test
  public void publicField() throws Exception {
    Configuration freemarker =
        FreemarkerModule.create()
            .build(new Environment(getClass().getClassLoader(), ConfigFactory.empty(), "test"));
    FreemarkerTemplateEngine engine = new FreemarkerTemplateEngine(freemarker, List.of(".ftl"));
    MockContext ctx =
        new MockContext().setRouter(new Jooby().setLocales(singletonList(Locale.ENGLISH)));
    ctx.getAttributes().put("local", "var");
    var output =
        engine.render(
            ctx,
            ModelAndView.map("index.ftl").put("user", new MyModel("foo", "bar")).put("sign", "!"));
    assertEquals(
        "Hello foo bar var!",
        StandardCharsets.UTF_8.decode(output.asByteBuffer()).toString().trim());
  }

  @Test
  public void customTemplatePath() throws Exception {
    Configuration freemarker =
        FreemarkerModule.create()
            .build(
                new Environment(
                    getClass().getClassLoader(),
                    ConfigFactory.empty()
                        .withValue("templates.path", ConfigValueFactory.fromAnyRef("foo"))));
    FreemarkerTemplateEngine engine = new FreemarkerTemplateEngine(freemarker, List.of(".ftl"));
    MockContext ctx =
        new MockContext().setRouter(new Jooby().setLocales(singletonList(Locale.ENGLISH)));
    ctx.getAttributes().put("local", "var");
    var output = engine.render(ctx, ModelAndView.map("index.ftl"));
    assertEquals("var", StandardCharsets.UTF_8.decode(output.asByteBuffer()).toString().trim());
  }
}
