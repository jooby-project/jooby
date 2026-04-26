/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.pac4j;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.pac4j.core.authorization.authorizer.Authorizer;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.http.client.direct.DirectBasicAuthClient;

import io.jooby.Jooby;
import io.jooby.ServiceRegistry;
import io.jooby.StatusCode;
import io.jooby.internal.pac4j.ForwardingAuthorizer;
import io.jooby.internal.pac4j.SecurityFilterImpl;

public class Pac4jModuleTest {

  private Jooby app;
  private ServiceRegistry registry;
  private com.typesafe.config.Config config;

  @BeforeEach
  void setUp() {
    app = mock(Jooby.class);
    registry = mock(ServiceRegistry.class);
    config = mock(com.typesafe.config.Config.class);

    when(app.getServices()).thenReturn(registry);
    when(app.getConfig()).thenReturn(config);
    when(app.getContextPath()).thenReturn("/");
  }

  @Test
  void testConstructors() {
    assertNotNull(new Pac4jModule());
    assertNotNull(new Pac4jModule(new Pac4jOptions()));
    assertNotNull(new Pac4jModule(new Config()));
  }

  @Test
  void testClientDSLVariants() {
    Pac4jModule module = new Pac4jModule();
    Authorizer mockAuthorizer = mock(Authorizer.class);
    // Use a real class or a mock that extends BaseClient to avoid Pac4j cast issues
    DirectBasicAuthClient mockClient = mock(DirectBasicAuthClient.class);
    Function<com.typesafe.config.Config, Client> provider = c -> mockClient;

    module.client(provider);
    module.client("/p1", provider);
    module.client(Authorizer.class, provider);
    module.client(mockAuthorizer, provider);
    module.client("/p2", Authorizer.class, provider);
    module.client("/p3", mockAuthorizer, provider);

    module.client(DirectBasicAuthClient.class);
    module.client("/p4", DirectBasicAuthClient.class);
    module.client(Authorizer.class, DirectBasicAuthClient.class);
    module.client(mockAuthorizer, DirectBasicAuthClient.class);
    module.client("/p5", Authorizer.class, DirectBasicAuthClient.class);
    module.client("/p6", mockAuthorizer, DirectBasicAuthClient.class);

    assertNotNull(module);
  }

  @Test
  void testInstallDefaultLogin() throws Exception {
    Pac4jModule module = new Pac4jModule();
    module.install(app);

    verify(app).get(eq("/login"), any());
    verify(app).get(eq("/callback"), any());
    verify(app).post(eq("/callback"), any());
  }

  @Test
  void testInstallWithResolvedClients() throws Exception {
    Pac4jOptions options = new Pac4jOptions();
    // Use a real client type for the mock to satisfy Pac4j's internal BaseClient casting
    DirectBasicAuthClient mockClient = mock(DirectBasicAuthClient.class);
    when(mockClient.getName()).thenReturn("test-client");
    options.setClients(new Clients(mockClient));

    Pac4jModule module = new Pac4jModule(options);
    module.install(app);

    assertEquals("test-client", options.getDefaultClient());
    verify(registry).put(eq(Config.class), eq(options));
  }

  @Test
  void testInstallWithUnresolvedClients() throws Exception {
    Pac4jModule module = new Pac4jModule();
    module.client("/secure", DirectBasicAuthClient.class);

    module.install(app);

    ArgumentCaptor<io.jooby.SneakyThrows.Runnable> captor =
        ArgumentCaptor.forClass(io.jooby.SneakyThrows.Runnable.class);
    verify(app).onStarting(captor.capture());

    DirectBasicAuthClient mockClient = mock(DirectBasicAuthClient.class);
    when(mockClient.getName()).thenReturn("lazy-client");
    when(app.require(DirectBasicAuthClient.class)).thenReturn(mockClient);

    captor.getValue().run();
  }

  @Test
  void testPatternHandling() throws Exception {
    Pac4jModule module = new Pac4jModule();
    // Using DirectClient so callback routes aren't forced, keeping verification clean
    module.client("/api/:id", DirectBasicAuthClient.class);
    module.client("/static", DirectBasicAuthClient.class);

    module.install(app);

    // Based on actual invocations: path keys use get/post, not use()
    verify(app).get(eq("/api/:id"), any(SecurityFilterImpl.class));
    verify(app).post(eq("/api/:id"), any(SecurityFilterImpl.class));
    verify(app).get(eq("/static"), any(SecurityFilterImpl.class));
    verify(app).post(eq("/static"), any(SecurityFilterImpl.class));
  }

  @Test
  void testForwardingAuthorizerInjection() throws Exception {
    Pac4jModule module = new Pac4jModule();
    module.client("/secure", Authorizer.class, DirectBasicAuthClient.class);

    module.install(app);

    Authorizer authorizer = ((Pac4jOptions) module.options()).getAuthorizers().get("Authorizer");
    assertTrue(authorizer instanceof ForwardingAuthorizer);
  }

  @Test
  void testStaticMethods() {
    assertNotNull(Pac4jModule.newLogoutLogic());
    assertNotNull(Pac4jModule.newActionAdapter());
    assertNotNull(Pac4jModule.newSecurityLogic(Collections.emptySet()));
    assertNotNull(Pac4jModule.newCallbackLogic(Collections.emptySet()));
    assertNotNull(Pac4jModule.newUrlResolver());
  }

  @Test
  void testErrorCodeRegistration() throws Exception {
    Pac4jModule module = new Pac4jModule();
    module.install(app);

    verify(app)
        .errorCode(org.pac4j.core.exception.http.UnauthorizedAction.class, StatusCode.UNAUTHORIZED);
    verify(app)
        .errorCode(org.pac4j.core.exception.http.ForbiddenAction.class, StatusCode.FORBIDDEN);
  }
}
