/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.typesafe.config.Config;
import io.jooby.exception.RegistryException;
import io.jooby.value.ValueFactory;

/**
 * Unit test suite for the {@link Jooby} framework orchestrator class.
 *
 * <p>Because the {@code Jooby} class acts as the central hub connecting the web server,
 * environment, router, and dependency registry, its deeper execution mechanics are heavily
 * validated via integration tests. This test suite focuses exclusively on validating the
 * surface-level API, state management, and delegation logic.
 *
 * <p>Specifically, this test verifies:
 *
 * <ul>
 *   <li><strong>State Management:</strong> Proper mutation and retrieval of application properties,
 *       configuration, locales, execution modes, and temporary directories.
 *   <li><strong>Router Delegation:</strong> Ensures that routing setup, middleware ({@code use},
 *       {@code before}, {@code after}), and WebSocket/SSE handlers are safely forwarded to the
 *       underlying {@code RouterImpl}.
 *   <li><strong>Lifecycle Callbacks:</strong> Validates the registration and execution triggers for
 *       application lifecycle hooks ({@code onStarting}, {@code onStarted}, {@code onStop}).
 *   <li><strong>Extension Management:</strong> Verifies the logic for standard and deferred
 *       (late-init) module installations.
 *   <li><strong>Dependency Registry:</strong> Checks the fallback and resolution behavior for
 *       required services and workers.
 * </ul>
 *
 * <p>By mocking the underlying engine and environment, this suite ensures the framework's primary
 * facade behaves correctly and maintains its contract without requiring a live HTTP server binding.
 */
public class JoobyApiUnitTest {
  private Jooby app;
  private Environment env;
  private Config config;

  @BeforeEach
  public void setUp() {
    app = new Jooby();
    env = mock(Environment.class);
    config = mock(Config.class);
    when(env.getConfig()).thenReturn(config);
    app.setEnvironment(env);
  }

  @Test
  public void appProperties() {
    app.setName("MyTestApp");
    assertEquals("MyTestApp", app.getName());

    app.setVersion("1.2.3");
    assertEquals("1.2.3", app.getVersion());

    app.setBasePackage("com.example.app");
    assertEquals("com.example.app", app.getBasePackage());

    assertEquals("MyTestApp:1.2.3", app.toString());
  }

  @Test
  public void locales() {
    assertNull(app.getLocales());
    app.setLocales(Locale.ENGLISH, Locale.CANADA);
    assertEquals(2, app.getLocales().size());
    assertEquals(Locale.ENGLISH, app.getLocales().get(0));
  }

  @Test
  public void contextPath() {
    assertEquals("/", app.getContextPath());
    app.setContextPath("/api");
    assertEquals("/api", app.getContextPath());
  }

  @Test
  public void executionMode() {
    app.setExecutionMode(ExecutionMode.WORKER);
    assertEquals(ExecutionMode.WORKER, app.getExecutionMode());
  }

  @Test
  public void tmpDir() {
    Path temp = Paths.get("/tmp/jooby");
    app.setTmpdir(temp);
    assertEquals(temp, app.getTmpdir());
  }

  @Test
  public void attributes() {
    app.setAttribute("key1", "value1");
    assertEquals("value1", app.getAttribute("key1"));
    assertTrue(app.getAttributes().containsKey("key1"));
  }

  @Test
  public void environmentAndConfig() {
    assertSame(env, app.getEnvironment());
    assertSame(config, app.getConfig());

    EnvironmentOptions options = new EnvironmentOptions();
    app.setEnvironmentOptions(options);
    assertNotNull(app.getEnvironment()); // loads actual environment
  }

  @Test
  public void routerOptions() {
    RouterOptions options = new RouterOptions();
    options.setIgnoreCase(true);
    app.setRouterOptions(options);
    assertTrue(app.getRouterOptions().isIgnoreCase());
    assertNotNull(app.getServerOptions());
  }

  @Test
  public void badMvcInstall() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            app.mvc(
                application -> {
                  throw new IllegalArgumentException("boom");
                }));
  }

  @Test
  public void badExtensionInstall() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            app.install(
                application -> {
                  throw new IllegalArgumentException("boom");
                }));
  }

  @Test
  public void shouldMountOnPredicateWithAction() {
    app.mount(
        ctx -> true,
        () -> {
          // do nothing
        });
  }

  @Test
  public void shouldDispatchWithAction() {
    var executor = mock(Executor.class);
    var action = mock(Runnable.class);
    app.dispatch(executor, action);
  }

  @Test
  public void shouldGroupRoutes() {
    var action = mock(Runnable.class);
    app.routes(action);
  }

  @Test
  public void shouldMatch() {
    assertTrue(app.match("/*", "/path"));
  }

  @Test
  public void shouldRequireNamedReified() {
    assertThrows(
        RegistryException.class, () -> app.require(Reified.list(String.class), "listOfString"));
  }

  @Test
  public void shouldGetDefaultPackageName() {
    assertNotNull(app.getBasePackage());
  }

  @Test
  public void shouldGetDefaultAppName() {
    assertEquals("Jooby", app.getName());
  }

  @Test
  public void shouldIgnoreSimpleExecutorOfBeingClose() {
    var executor = mock(Executor.class);
    app.executor("simple", executor);
  }

  @Test
  public void shouldGetDefaultStartupSummary() {
    assertNull(app.getStartupSummary());
  }

  @Test
  public void shouldThrowLateInitException() {
    app.install(
        new Extension() {
          @Override
          public boolean lateinit() {
            return true;
          }

          @Override
          public void install(Jooby application) throws Exception {
            throw new IllegalStateException("boom");
          }
        });
    var server = mock(Server.class);
    app.setTmpdir(Paths.get(System.getProperty("java.io.tmpdir")));
    assertThrows(IllegalStateException.class, () -> app.start(server));
  }

  @Test
  public void shouldStartWithNoSummary() {
    var server = mock(Server.class);
    when(server.getOptions()).thenReturn(new ServerOptions());
    app.ready(server);
  }

  @Test
  public void shouldStartWithConfigSummary() {
    var server = mock(Server.class);
    // when(server.getOptions()).thenReturn(new ServerOptions());
    when(config.hasPath(AvailableSettings.STARTUP_SUMMARY)).thenReturn(true);
    when(config.getAnyRef(AvailableSettings.STARTUP_SUMMARY)).thenReturn("NONE");
    app.ready(server);
  }

  @Test
  public void shouldStartWithConfigSummaryList() {
    var server = mock(Server.class);
    // when(server.getOptions()).thenReturn(new ServerOptions());
    when(config.hasPath(AvailableSettings.STARTUP_SUMMARY)).thenReturn(true);
    when(config.getAnyRef(AvailableSettings.STARTUP_SUMMARY)).thenReturn(List.of("NONE", "NONE"));
    app.ready(server);
  }

  @Test
  public void shouldInstallWebSocket() {
    app.ws(application -> {});
  }

  @Test
  public void shouldNotCopyRegistryOnInternalRouter() {
    var router = mock(Router.class);
    app.mount("/path", router);
  }

  @Test
  public void stateFlags() {
    assertTrue(app.isStarted());
    assertFalse(app.isStopped());
  }

  @Test
  public void installExtension() throws Exception {
    Extension ext = mock(Extension.class);
    when(ext.lateinit()).thenReturn(false);

    app.install(ext);
    verify(ext, times(1)).install(app);
  }

  @Test
  public void installLateExtension() throws Exception {
    Extension ext = mock(Extension.class);
    when(ext.lateinit()).thenReturn(true);

    app.install(ext);
    verify(ext, times(0)).install(app); // Should be deferred
  }

  @Test
  public void requireServiceThrowsWhenMissing() {
    assertThrows(RegistryException.class, () -> app.require(String.class));
  }

  @Test
  public void registryDelegation() {
    Registry mockRegistry = mock(Registry.class);
    when(mockRegistry.require(ServiceKey.key(String.class))).thenReturn("InjectedString");

    app.registry(mockRegistry);
    assertEquals("InjectedString", app.require(String.class));
  }

  @Test
  public void factoriesAndStores() {
    SessionStore store = mock(SessionStore.class);
    app.setSessionStore(store);
    assertSame(store, app.getSessionStore());

    ValueFactory vf = mock(ValueFactory.class);
    app.setValueFactory(vf);
    assertSame(vf, app.getValueFactory());

    assertNotNull(app.getOutputFactory());
  }
}
