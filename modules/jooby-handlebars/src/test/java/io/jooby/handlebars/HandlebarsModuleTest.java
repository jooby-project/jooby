/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.handlebars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.ValueResolver;
import com.github.jknack.handlebars.cache.HighConcurrencyTemplateCache;
import com.github.jknack.handlebars.cache.NullTemplateCache;
import com.github.jknack.handlebars.cache.TemplateCache;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.FileTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import io.jooby.Environment;
import io.jooby.Jooby;
import io.jooby.ServiceRegistry;
import io.jooby.TemplateEngine;

class HandlebarsModuleTest {

  private Jooby app;
  private Environment env;
  private ServiceRegistry registry;

  @BeforeEach
  void setUp() {
    app = mock(Jooby.class);
    env = mock(Environment.class);
    registry = mock(ServiceRegistry.class);

    when(app.getEnvironment()).thenReturn(env);
    when(app.getServices()).thenReturn(registry);
    when(env.getProperty(eq(TemplateEngine.TEMPLATE_PATH), anyString()))
        .thenAnswer(inv -> inv.getArgument(1));
    when(env.getClassLoader()).thenReturn(getClass().getClassLoader());
  }

  // --- CONSTRUCTOR & INSTALLATION TESTS ---

  @Test
  void testInstallWithExistingHandlebars() throws Exception {
    Handlebars customHbs = new Handlebars();
    HandlebarsModule module = new HandlebarsModule(customHbs);

    module.install(app);

    verify(app).encoder(any());
    verify(registry).put(Handlebars.class, customHbs);
  }

  @Test
  void testInstallWithDefaultPath() throws Exception {
    HandlebarsModule module = new HandlebarsModule();

    module.install(app);

    verify(app).encoder(any());
    verify(registry).put(eq(Handlebars.class), any(Handlebars.class));
  }

  @Test
  void testInstallWithStringPath() throws Exception {
    HandlebarsModule module = new HandlebarsModule("custom_views");

    module.install(app);

    verify(app).encoder(any());
    verify(registry).put(eq(Handlebars.class), any(Handlebars.class));
  }

  @Test
  void testInstallWithNioPath(@TempDir Path tempDir) throws Exception {
    HandlebarsModule module = new HandlebarsModule(tempDir);

    module.install(app);

    verify(app).encoder(any());
    verify(registry).put(eq(Handlebars.class), any(Handlebars.class));
  }

  @Test
  void testInstallWithCustomValueResolver() throws Exception {
    ValueResolver customResolver = mock(ValueResolver.class);
    HandlebarsModule module = new HandlebarsModule().with(customResolver);

    module.install(app);

    verify(app).encoder(any());
    verify(registry).put(eq(Handlebars.class), any(Handlebars.class));
  }

  // --- BUILDER TESTS ---

  @Test
  void testBuilderWithCustomTemplateLoader() {
    TemplateLoader stringLoader = new FileTemplateLoader("/some");
    Handlebars hbs = HandlebarsModule.create().setTemplateLoader(stringLoader).build(env);

    assertEquals(stringLoader, hbs.getLoader());
  }

  @Test
  void testBuilderWithCustomTemplateCache() {
    TemplateCache customCache = mock(TemplateCache.class);
    Handlebars hbs = HandlebarsModule.create().setTemplateCache(customCache).build(env);

    assertEquals(customCache, hbs.getCache());
  }

  @Test
  void testBuilderCacheInDevMode() {
    when(env.isActive("dev", "test")).thenReturn(true);

    Handlebars hbs = HandlebarsModule.create().build(env);

    assertEquals(NullTemplateCache.INSTANCE, hbs.getCache());
  }

  @Test
  void testBuilderCacheInProdMode() {
    when(env.isActive("dev", "test")).thenReturn(false);

    Handlebars hbs = HandlebarsModule.create().build(env);

    assertTrue(hbs.getCache() instanceof HighConcurrencyTemplateCache);
  }

  // --- DEFAULT TEMPLATE LOADER RESOLUTION TESTS ---

  @Test
  void testDefaultTemplateLoaderFileSystemFallback(@TempDir Path tempDir) {
    Handlebars hbs = HandlebarsModule.create().setTemplatesPath(tempDir).build(env);

    // Temp directory exists, should map to FileTemplateLoader
    assertTrue(hbs.getLoader() instanceof FileTemplateLoader);
  }

  @Test
  void testDefaultTemplateLoaderClasspathFallback() {
    Handlebars hbs =
        HandlebarsModule.create()
            .setTemplatesPath("this_path_does_not_exist_on_file_system")
            .build(env);

    // Path doesn't exist, should map to ClassPathTemplateLoader
    assertTrue(hbs.getLoader() instanceof ClassPathTemplateLoader);
  }

  @Test
  void testClassPathTemplateLoaderResourceResolution() throws IOException {
    Handlebars hbs =
        HandlebarsModule.create()
            .setTemplatesPath("this_path_does_not_exist_on_file_system")
            .build(env);

    ClassPathTemplateLoader loader = (ClassPathTemplateLoader) hbs.getLoader();

    // Test the overridden getResource method uses the Environment's ClassLoader
    URL resourceUrl = new URL("file:///dummy");
    ClassLoader classLoader = mock(ClassLoader.class);
    when(env.getClassLoader()).thenReturn(classLoader);
    when(classLoader.getResource("test.hbs")).thenReturn(resourceUrl);
  }
}
