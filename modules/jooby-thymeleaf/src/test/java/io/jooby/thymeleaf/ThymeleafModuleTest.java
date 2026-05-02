/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.thymeleaf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.cache.ICacheManager;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.AbstractConfigurableTemplateResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import io.jooby.Environment;
import io.jooby.Jooby;
import io.jooby.ServiceRegistry;
import io.jooby.internal.thymeleaf.ThymeleafTemplateEngine;

@ExtendWith(MockitoExtension.class)
class ThymeleafModuleTest {

  @Mock Jooby app;
  @Mock Environment env;
  @Mock ServiceRegistry registry;

  @BeforeEach
  void setup() {
    lenient().when(app.getEnvironment()).thenReturn(env);
    lenient().when(app.getServices()).thenReturn(registry);

    // Make getProperty pass through the provided default value to simulate standard behavior
    lenient()
        .when(env.getProperty(eq(io.jooby.TemplateEngine.TEMPLATE_PATH), anyString()))
        .thenAnswer(inv -> inv.getArgument(1));

    // Setup ClassLoader mock for ClassLoaderTemplateResolver branch
    lenient().when(env.getClassLoader()).thenReturn(getClass().getClassLoader());
  }

  // --- CONSTRUCTOR & INSTALL LIFECYCLE TESTS ---

  @Test
  void testDefaultConstructorInstall() {
    // Tests: new ThymeleafModule() -> defaults to TemplateEngine.PATH ("views")
    ThymeleafModule module = new ThymeleafModule();
    module.install(app);

    verify(app).encoder(any(ThymeleafTemplateEngine.class));
    verify(registry).put(eq(TemplateEngine.class), any(TemplateEngine.class));
  }

  @Test
  void testStringPathConstructorInstall() {
    // Tests: new ThymeleafModule(String path)
    ThymeleafModule module = new ThymeleafModule("custom-views-dir");
    module.install(app);

    verify(app).encoder(any(ThymeleafTemplateEngine.class));
    verify(registry).put(eq(TemplateEngine.class), any(TemplateEngine.class));
  }

  @Test
  void testPathObjectConstructorInstall(@TempDir Path tempDir) {
    // Tests: new ThymeleafModule(Path path)
    ThymeleafModule module = new ThymeleafModule(tempDir);
    module.install(app);

    verify(app).encoder(any(ThymeleafTemplateEngine.class));
    verify(registry).put(eq(TemplateEngine.class), any(TemplateEngine.class));
  }

  @Test
  void testTemplateEngineConstructorInstall() {
    // Tests: new ThymeleafModule(TemplateEngine engine)
    TemplateEngine mockEngine = new TemplateEngine();
    ThymeleafModule module = new ThymeleafModule(mockEngine);

    module.install(app);

    verify(app).encoder(any(ThymeleafTemplateEngine.class));

    // Verify it registered the exact instance provided
    verify(registry).put(TemplateEngine.class, mockEngine);
  }

  // --- BUILDER CONFIGURATION TESTS ---

  @Test
  void testBuilder_CustomTemplateResolver() {
    ITemplateResolver customResolver = mock(ITemplateResolver.class);

    TemplateEngine engine = ThymeleafModule.create().setTemplateResolver(customResolver).build(env);

    Set<ITemplateResolver> resolvers = engine.getTemplateResolvers();
    assertEquals(1, resolvers.size());
    assertTrue(resolvers.contains(customResolver));
  }

  @Test
  void testBuilder_CustomCacheManager() {
    ICacheManager customCache = mock(ICacheManager.class);

    TemplateEngine engine = ThymeleafModule.create().setCacheManager(customCache).build(env);

    assertEquals(customCache, engine.getCacheManager());
  }

  @Test
  void testBuilder_CacheableExplicitlyTrue() {
    TemplateEngine engine = ThymeleafModule.create().setCacheable(true).build(env);

    AbstractConfigurableTemplateResolver resolver =
        (AbstractConfigurableTemplateResolver) engine.getTemplateResolvers().iterator().next();
    assertTrue(resolver.isCacheable());
    assertEquals(TemplateMode.HTML, resolver.getTemplateMode());
  }

  @Test
  void testBuilder_CacheableExplicitlyFalse() {
    TemplateEngine engine = ThymeleafModule.create().setCacheable(false).build(env);

    AbstractConfigurableTemplateResolver resolver =
        (AbstractConfigurableTemplateResolver) engine.getTemplateResolvers().iterator().next();
    assertFalse(resolver.isCacheable());
  }

  @Test
  void testBuilder_CacheableNull_EnvActiveDevOrTest() {
    // If cacheable is null, it checks env.isActive("dev", "test"). If true, cache is OFF.
    when(env.isActive("dev", "test")).thenReturn(true);

    TemplateEngine engine = ThymeleafModule.create().build(env);

    AbstractConfigurableTemplateResolver resolver =
        (AbstractConfigurableTemplateResolver) engine.getTemplateResolvers().iterator().next();
    assertFalse(resolver.isCacheable());
  }

  @Test
  void testBuilder_CacheableNull_EnvNotActiveDevOrTest() {
    // If cacheable is null and env is NOT dev/test, cache is ON.
    when(env.isActive("dev", "test")).thenReturn(false);

    TemplateEngine engine = ThymeleafModule.create().build(env);

    AbstractConfigurableTemplateResolver resolver =
        (AbstractConfigurableTemplateResolver) engine.getTemplateResolvers().iterator().next();
    assertTrue(resolver.isCacheable());
  }

  // --- TEMPLATE RESOLVER SELECTION TESTS ---

  @Test
  void testBuilder_ResolvesToFileTemplateResolver_IfPathExists(@TempDir Path tempDir) {
    // Inject the physical temp directory
    TemplateEngine engine = ThymeleafModule.create().setTemplatesPath(tempDir).build(env);

    ITemplateResolver rawResolver = engine.getTemplateResolvers().iterator().next();
    assertTrue(rawResolver instanceof FileTemplateResolver);

    FileTemplateResolver resolver = (FileTemplateResolver) rawResolver;
    assertEquals(tempDir.toAbsolutePath().toString(), resolver.getPrefix());
    assertFalse(resolver.getForceSuffix());
  }

  @Test
  void testBuilder_ResolvesToClassLoader_IfPathDoesNotExist_WithoutSlash() {
    // A path that definitely doesn't exist on the physical filesystem
    String fakePath = "non-existent-classpath-dir-123";

    TemplateEngine engine = ThymeleafModule.create().setTemplatesPath(fakePath).build(env);

    ITemplateResolver rawResolver = engine.getTemplateResolvers().iterator().next();
    assertTrue(rawResolver instanceof ClassLoaderTemplateResolver);

    ClassLoaderTemplateResolver resolver = (ClassLoaderTemplateResolver) rawResolver;

    // Because fakePath does NOT start with "/", it should prepend one
    assertEquals("/" + fakePath, resolver.getPrefix());
  }

  @Test
  void testBuilder_ResolvesToClassLoader_IfPathDoesNotExist_WithSlash() {
    // A path with a leading slash that doesn't exist physically
    String fakePath = "/non-existent-classpath-dir-123";

    TemplateEngine engine = ThymeleafModule.create().setTemplatesPath(fakePath).build(env);

    ITemplateResolver rawResolver = engine.getTemplateResolvers().iterator().next();
    assertTrue(rawResolver instanceof ClassLoaderTemplateResolver);

    ClassLoaderTemplateResolver resolver = (ClassLoaderTemplateResolver) rawResolver;

    // Because fakePath starts with "/", it should use it as-is
    assertEquals(fakePath, resolver.getPrefix());
  }
}
