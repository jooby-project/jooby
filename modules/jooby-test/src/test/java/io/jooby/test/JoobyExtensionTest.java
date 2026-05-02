/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Parameter;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;

import com.typesafe.config.Config;
import io.jooby.Environment;
import io.jooby.Jooby;
import io.jooby.Server;
import io.jooby.ServerOptions;

class JoobyExtensionTest {

  private ExtensionContext context;
  private ExtensionContext.Store store;

  @BeforeEach
  void setUp() {
    context = mock(ExtensionContext.class);
    store = mock(ExtensionContext.Store.class);

    when(context.getRequiredTestClass()).thenReturn((Class) Object.class);
    when(context.getStore(any())).thenReturn(store);
  }

  @Test
  void shouldSupportJoobyParameter() throws ParameterResolutionException {
    ParameterContext parameterContext = mock(ParameterContext.class);
    Parameter parameter = mock(Parameter.class);
    when(parameterContext.getParameter()).thenReturn(parameter);
    when(parameter.getType()).thenReturn((Class) Jooby.class);

    JoobyExtension extension = new JoobyExtension();
    assertTrue(extension.supportsParameter(parameterContext, context));
  }

  @Test
  void shouldSupportEnvironmentParameter() throws ParameterResolutionException {
    ParameterContext parameterContext = mock(ParameterContext.class);
    Parameter parameter = mock(Parameter.class);
    when(parameterContext.getParameter()).thenReturn(parameter);
    when(parameter.getType()).thenReturn((Class) Environment.class);

    JoobyExtension extension = new JoobyExtension();
    assertTrue(extension.supportsParameter(parameterContext, context));
  }

  @Test
  void shouldSupportConfigParameter() throws ParameterResolutionException {
    ParameterContext parameterContext = mock(ParameterContext.class);
    Parameter parameter = mock(Parameter.class);
    when(parameterContext.getParameter()).thenReturn(parameter);
    when(parameter.getType()).thenReturn((Class) Config.class);

    JoobyExtension extension = new JoobyExtension();
    assertTrue(extension.supportsParameter(parameterContext, context));
  }

  @Test
  void shouldSupportServerPathParameter() throws ParameterResolutionException {
    ParameterContext parameterContext = mock(ParameterContext.class);
    Parameter parameter = mock(Parameter.class);
    when(parameterContext.getParameter()).thenReturn(parameter);
    when(parameter.getType()).thenReturn((Class) String.class);
    when(parameter.isNamePresent()).thenReturn(true);
    when(parameter.getName()).thenReturn("serverPath");

    JoobyExtension extension = new JoobyExtension();
    assertTrue(extension.supportsParameter(parameterContext, context));
  }

  @Test
  void shouldSupportServerPortParameter() throws ParameterResolutionException {
    ParameterContext parameterContext = mock(ParameterContext.class);
    Parameter parameter = mock(Parameter.class);
    when(parameterContext.getParameter()).thenReturn(parameter);
    when(parameter.getType()).thenReturn((Class) int.class);
    when(parameter.isNamePresent()).thenReturn(true);
    when(parameter.getName()).thenReturn("serverPort");

    JoobyExtension extension = new JoobyExtension();
    assertTrue(extension.supportsParameter(parameterContext, context));
  }

  @Test
  void shouldNotSupportUnknownParameter() throws ParameterResolutionException {
    ParameterContext parameterContext = mock(ParameterContext.class);
    Parameter parameter = mock(Parameter.class);
    when(parameterContext.getParameter()).thenReturn(parameter);
    when(parameter.getType()).thenReturn((Class) Object.class);

    JoobyExtension extension = new JoobyExtension();
    assertFalse(extension.supportsParameter(parameterContext, context));
  }

  @Test
  void shouldThrowExceptionWhenParameterNameIsMissing() {
    ParameterContext parameterContext = mock(ParameterContext.class);
    Parameter parameter = mock(Parameter.class);
    when(parameterContext.getParameter()).thenReturn(parameter);
    when(parameter.getType()).thenReturn((Class) String.class);
    when(parameter.isNamePresent()).thenReturn(false);

    JoobyExtension extension = new JoobyExtension();

    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () -> extension.supportsParameter(parameterContext, context));
    assertTrue(ex.getMessage().contains("parameter names to be present"));
  }

  @Test
  void shouldResolveJoobyParameter() throws ParameterResolutionException {
    ParameterContext parameterContext = mock(ParameterContext.class);
    Parameter parameter = mock(Parameter.class);
    when(parameterContext.getParameter()).thenReturn(parameter);
    when(parameter.getType()).thenReturn((Class) Jooby.class);

    Jooby app = new Jooby();
    when(store.get("application", Jooby.class)).thenReturn(app);

    JoobyExtension extension = new JoobyExtension();
    assertEquals(app, extension.resolveParameter(parameterContext, context));
  }

  @Test
  void shouldResolveEnvironmentParameter() throws ParameterResolutionException {
    ParameterContext parameterContext = mock(ParameterContext.class);
    Parameter parameter = mock(Parameter.class);
    when(parameterContext.getParameter()).thenReturn(parameter);
    when(parameter.getType()).thenReturn((Class) Environment.class);

    Jooby app = new Jooby();
    Environment env = mock(Environment.class);
    app.setEnvironment(env);

    when(store.get("application", Jooby.class)).thenReturn(app);

    JoobyExtension extension = new JoobyExtension();
    assertEquals(env, extension.resolveParameter(parameterContext, context));
  }

  @Test
  void shouldResolveConfigParameter() throws ParameterResolutionException {
    ParameterContext parameterContext = mock(ParameterContext.class);
    Parameter parameter = mock(Parameter.class);
    when(parameterContext.getParameter()).thenReturn(parameter);
    when(parameter.getType()).thenReturn((Class) Config.class);

    Jooby app = new Jooby();
    Environment env = mock(Environment.class);
    Config config = mock(Config.class);
    when(env.getConfig()).thenReturn(config);
    app.setEnvironment(env);

    when(store.get("application", Jooby.class)).thenReturn(app);

    JoobyExtension extension = new JoobyExtension();
    assertEquals(config, extension.resolveParameter(parameterContext, context));
  }

  @Test
  void shouldResolveServerPathParameter() throws ParameterResolutionException {
    ParameterContext parameterContext = mock(ParameterContext.class);
    Parameter parameter = mock(Parameter.class);
    when(parameterContext.getParameter()).thenReturn(parameter);
    when(parameter.getType()).thenReturn((Class) String.class);
    when(parameter.isNamePresent()).thenReturn(true);
    when(parameter.getName()).thenReturn("serverPath");

    Jooby app = new Jooby();
    app.setContextPath("/app");

    Server server = mock(Server.class);
    ServerOptions options = new ServerOptions();
    options.setPort(8080);
    when(server.getOptions()).thenReturn(options);

    when(store.get("application", Jooby.class)).thenReturn(app);
    when(store.get("server", Server.class)).thenReturn(server);

    JoobyExtension extension = new JoobyExtension();
    assertEquals(
        "http://localhost:8080/app", extension.resolveParameter(parameterContext, context));
  }

  @Test
  void shouldResolveServerPortParameter() throws ParameterResolutionException {
    ParameterContext parameterContext = mock(ParameterContext.class);
    Parameter parameter = mock(Parameter.class);
    when(parameterContext.getParameter()).thenReturn(parameter);
    when(parameter.getType()).thenReturn((Class) int.class);
    when(parameter.isNamePresent()).thenReturn(true);
    when(parameter.getName()).thenReturn("serverPort");

    Server server = mock(Server.class);
    ServerOptions options = new ServerOptions();
    options.setPort(9090);
    when(server.getOptions()).thenReturn(options);

    when(store.get("server", Server.class)).thenReturn(server);

    JoobyExtension extension = new JoobyExtension();
    assertEquals(9090, extension.resolveParameter(parameterContext, context));
  }

  @Test
  void shouldThrowExceptionWhenServiceNotFoundInStoreOrParent() {
    ParameterContext parameterContext = mock(ParameterContext.class);
    Parameter parameter = mock(Parameter.class);
    when(parameterContext.getParameter()).thenReturn(parameter);
    when(parameter.getType()).thenReturn((Class) Jooby.class);

    when(store.get("application", Jooby.class)).thenReturn(null);
    when(context.getParent()).thenReturn(Optional.empty());

    JoobyExtension extension = new JoobyExtension();

    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () -> extension.resolveParameter(parameterContext, context));
    assertTrue(ex.getMessage().contains("Not found: Jooby"));
  }

  @Test
  void shouldResolveServiceFromParentContextIfMissingInCurrent()
      throws ParameterResolutionException {
    ParameterContext parameterContext = mock(ParameterContext.class);
    Parameter parameter = mock(Parameter.class);
    when(parameterContext.getParameter()).thenReturn(parameter);
    when(parameter.getType()).thenReturn((Class) Jooby.class);

    ExtensionContext parentContext = mock(ExtensionContext.class);
    ExtensionContext.Store parentStore = mock(ExtensionContext.Store.class);

    when(parentContext.getRequiredTestClass()).thenReturn((Class) Object.class);

    when(store.get("application", Jooby.class)).thenReturn(null);
    when(context.getParent()).thenReturn(Optional.of(parentContext));
    when(parentContext.getStore(any())).thenReturn(parentStore);

    Jooby app = new Jooby();
    when(parentStore.get("application", Jooby.class)).thenReturn(app);

    JoobyExtension extension = new JoobyExtension();
    assertEquals(app, extension.resolveParameter(parameterContext, context));
  }
}
