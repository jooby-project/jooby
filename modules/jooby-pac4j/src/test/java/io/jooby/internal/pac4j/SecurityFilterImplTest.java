/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.pac4j;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pac4j.core.client.finder.DefaultSecurityClientFinder;
import org.pac4j.core.context.WebContextFactory;
import org.pac4j.core.engine.DefaultSecurityLogic;
import org.pac4j.core.engine.SecurityLogic;

import io.jooby.Context;
import io.jooby.Route;
import io.jooby.Router;
import io.jooby.ServiceRegistry;
import io.jooby.pac4j.Pac4jOptions;
import io.jooby.value.Value;
import io.jooby.value.ValueFactory;

public class SecurityFilterImplTest {

  private Pac4jOptions options;
  private SecurityLogic securityLogic;
  private Context ctx;
  private Route.Handler next;

  @BeforeEach
  void setUp() {
    options = new Pac4jOptions();
    // Pac4j's DefaultSecurityLogic requires a WebContextFactory
    options.setWebContextFactory(mock(WebContextFactory.class));

    securityLogic = mock(SecurityLogic.class);
    options.setSecurityLogic(securityLogic);

    ctx = mock(Context.class);
    next = mock(Route.Handler.class);

    // Mock Router/Registry for Pac4jFrameworkParameters
    Router router = mock(Router.class);
    when(ctx.getRouter()).thenReturn(router);
    when(router.getServices()).thenReturn(mock(ServiceRegistry.class));
  }

  @Test
  void testAddAuthorizer() throws Exception {
    SecurityFilterImpl filter =
        new SecurityFilterImpl(null, options, () -> "c1", new ArrayList<>());
    filter.addAuthorizer("a1");
    filter.addAuthorizer("a2");

    when(ctx.lookup(anyString())).thenReturn(Value.missing(new ValueFactory(), "client"));
    filter.apply(ctx);

    // Verify concatenated string "a1,a2" (Pac4jConstants.ELEMENT_SEPARATOR is usually comma)
    verify(securityLogic).perform(any(), any(), any(), eq("a1,a2"), any(), any());
  }

  @Test
  void testFilterPatternMatches() throws Exception {
    SecurityFilterImpl filter =
        new SecurityFilterImpl("/secure/*", options, () -> "c1", List.of("a1"));
    when(ctx.matches("/secure/*")).thenReturn(true);
    when(ctx.lookup(anyString())).thenReturn(Value.missing(new ValueFactory(), "client"));

    filter.apply(next).apply(ctx);

    verify(securityLogic).perform(eq(options), any(), eq("c1"), eq("a1"), any(), any());
  }

  @Test
  void testFilterPatternDoesNotMatch() throws Exception {
    SecurityFilterImpl filter =
        new SecurityFilterImpl("/secure/*", options, () -> "c1", List.of("a1"));
    when(ctx.matches("/secure/*")).thenReturn(false);

    filter.apply(next).apply(ctx);

    verify(next).apply(ctx);
    verifyNoInteractions(securityLogic);
  }

  @Test
  void testHandlerMode() throws Exception {
    SecurityFilterImpl filter = new SecurityFilterImpl(null, options, () -> "c1", List.of());
    when(ctx.lookup(anyString())).thenReturn(Value.missing(new ValueFactory(), "client"));

    filter.apply(ctx);

    verify(securityLogic)
        .perform(eq(options), any(), eq("c1"), eq(NoopAuthorizer.NAME), any(), any());
  }

  @Test
  void testClientNameLookup() throws Exception {
    // 1. Setup a real DefaultSecurityLogic to exercise the clientName(securityLogic) branch
    DefaultSecurityLogic logic = new DefaultSecurityLogic();
    DefaultSecurityClientFinder finder = new DefaultSecurityClientFinder();
    finder.setClientNameParameter("custom_client");
    logic.setClientFinder(finder);

    // 2. We want to verify that when this logic is used, it looks up "custom_client"
    options.setSecurityLogic(logic);

    // 3. Mock the context to return a value for "custom_client"
    Value customClientValue = Value.value(new ValueFactory(), "custom_client", "c2");
    when(ctx.lookup("custom_client")).thenReturn(customClientValue);

    // 4. We still need to mock the perform call because DefaultSecurityLogic.perform
    // will eventually crash due to other missing pac4j dependencies.
    // So we use a spy on the real logic to intercept the perform call.
    DefaultSecurityLogic spyLogic = spy(logic);
    doReturn(null).when(spyLogic).perform(any(), any(), any(), any(), any(), any());
    options.setSecurityLogic(spyLogic);

    SecurityFilterImpl filter = new SecurityFilterImpl(null, options, () -> "c1", List.of());

    // Execute
    filter.apply(ctx);

    // 5. Verify that "c2" (the value from lookup) was passed to perform,
    // proving the custom client name was correctly identified and used.
    verify(spyLogic).perform(any(), any(), eq("c2"), any(), any(), any());
  }

  @Test
  void testMatchersConcatenation() throws Exception {
    options.setMatchers(Map.of("m1", mock(org.pac4j.core.matching.matcher.Matcher.class)));
    SecurityFilterImpl filter = new SecurityFilterImpl(null, options, () -> "c1", List.of());
    when(ctx.lookup(anyString())).thenReturn(Value.missing(new ValueFactory(), "client"));

    filter.apply(ctx);

    verify(securityLogic).perform(any(), any(), any(), any(), eq("m1"), any());
  }

  @Test
  void testExceptionPropagation() throws Exception {
    SecurityFilterImpl filter = new SecurityFilterImpl(null, options, () -> "c1", List.of());

    // 1. With Cause
    Exception cause = new Exception("fail");
    when(ctx.lookup(anyString())).thenThrow(new RuntimeException(cause));

    Exception ex = assertThrows(Exception.class, () -> filter.apply(ctx));
    assertEquals("fail", ex.getMessage());

    // 2. Without Cause
    reset(ctx);
    setupContext(ctx);
    when(ctx.lookup(anyString())).thenThrow(new RuntimeException("simple"));

    RuntimeException rex = assertThrows(RuntimeException.class, () -> filter.apply(ctx));
    assertEquals("simple", rex.getMessage());
  }

  private void setupContext(Context ctx) {
    Router router = mock(Router.class);
    when(ctx.getRouter()).thenReturn(router);
    when(router.getServices()).thenReturn(mock(ServiceRegistry.class));
  }
}
