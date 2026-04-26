/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.pac4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pac4j.core.config.Config;
import org.pac4j.core.engine.LogoutLogic;

import io.jooby.Context;
import io.jooby.Router;
import io.jooby.ServiceRegistry;
import io.jooby.pac4j.Pac4jOptions;

public class LogoutImplTest {

  private Config config;
  private LogoutLogic logoutLogic;
  private Pac4jOptions options;
  private Context ctx;
  private LogoutImpl logout;

  @BeforeEach
  void setUp() {
    config = mock(Config.class);
    logoutLogic = mock(LogoutLogic.class);
    options = new Pac4jOptions();
    ctx = mock(Context.class);

    // Mock the Router/Registry chain required by Pac4jFrameworkParameters.create(ctx)
    Router router = mock(Router.class);
    ServiceRegistry registry = mock(ServiceRegistry.class);
    when(ctx.getRouter()).thenReturn(router);
    when(router.getServices()).thenReturn(registry);

    when(config.getLogoutLogic()).thenReturn(logoutLogic);
    logout = new LogoutImpl(config, options);
  }

  @Test
  void testLogoutWithAttributeRedirect() throws Exception {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("pac4j.logout.redirectTo", "/success");
    when(ctx.getAttributes()).thenReturn(attributes);
    when(ctx.getRequestURL("/success")).thenReturn("http://localhost/success");

    logout.apply(ctx);

    verify(logoutLogic)
        .perform(
            eq(config),
            eq("http://localhost/success"),
            any(),
            anyBoolean(),
            anyBoolean(),
            anyBoolean(),
            any());
  }

  @Test
  void testLogoutWithDefaultUrl() throws Exception {
    options.setDefaultUrl("/default");
    // redirectTo is null
    when(ctx.getAttributes()).thenReturn(new HashMap<>());
    when(ctx.getRequestURL("/default")).thenReturn("http://localhost/default");

    logout.apply(ctx);

    verify(logoutLogic)
        .perform(
            eq(config),
            eq("http://localhost/default"),
            eq(options.getLogoutUrlPattern()),
            eq(options.isLocalLogout()),
            eq(options.isDestroySession()),
            eq(options.isCentralLogout()),
            any());
  }

  @Test
  void testLogoutWithEmptyAttributeRedirect() throws Exception {
    options.setDefaultUrl("/");
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("pac4j.logout.redirectTo", "");
    when(ctx.getAttributes()).thenReturn(attributes);
    when(ctx.getRequestURL("/")).thenReturn("http://localhost/");

    logout.apply(ctx);

    verify(logoutLogic)
        .perform(
            eq(config),
            eq("http://localhost/"),
            any(),
            anyBoolean(),
            anyBoolean(),
            anyBoolean(),
            any());
  }

  @Test
  void testExceptionPropagationWithCause() {
    Exception cause = new Exception("Real error");
    RuntimeException wrapper = new RuntimeException(cause);

    // Trigger exception inside the try block
    when(ctx.getAttributes()).thenThrow(wrapper);

    Exception result = assertThrows(Exception.class, () -> logout.apply(ctx));
    assertEquals("Real error", result.getMessage());
  }

  @Test
  void testExceptionPropagationWithoutCause() {
    RuntimeException simple = new RuntimeException("Simple error");

    when(ctx.getAttributes()).thenThrow(simple);

    RuntimeException result = assertThrows(RuntimeException.class, () -> logout.apply(ctx));
    assertEquals("Simple error", result.getMessage());
  }
}
