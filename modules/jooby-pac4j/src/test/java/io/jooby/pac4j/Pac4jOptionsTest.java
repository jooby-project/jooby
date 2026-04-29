/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.pac4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.pac4j.core.client.BaseClient;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.core.util.serializer.Serializer;

import io.jooby.SameSite;

public class Pac4jOptionsTest {
  final Set<String> CLONED =
      Set.of(
          "clients",
          "authorizers",
          "matchers",
          "securityLogic",
          "callbackLogic",
          "logoutLogic",
          "webContextFactory",
          "sessionStoreFactory",
          "profileManagerFactory",
          "httpActionAdapter",
          "sessionLogoutHandler");

  @Test
  public void mustCloneAllInstanceFieldOfConfig() {
    var existingFields =
        Stream.of(Config.class.getDeclaredFields())
            .filter(it -> !Modifier.isStatic(it.getModifiers()))
            .map(Field::getName)
            .collect(Collectors.toSet());
    existingFields.removeAll(CLONED);
    assertTrue(existingFields.isEmpty(), "Field must be copy: " + existingFields.toString());
  }

  @Test
  public void mustSetAllInstanceFieldOfConfig() throws Exception {
    var config = new Config();
    var values = new HashMap<String, Object>();
    for (String field : CLONED) {
      var type = Config.class.getDeclaredField(field).getType();
      var setter =
          Config.class.getMethod(
              "set" + Character.toUpperCase(field.charAt(0)) + field.substring(1), type);
      var value = mock(type);
      setter.invoke(config, value);
      values.put(field, value);
    }
    // This also tests the Pac4jOptions(Config) copy constructor's "ifPresent" true branches
    var options = Pac4jOptions.from(config);
    for (String field : CLONED) {
      var getter =
          Config.class.getMethod(
              "get" + Character.toUpperCase(field.charAt(0)) + field.substring(1));
      assertEquals(values.get(field), getter.invoke(options));
    }
  }

  @Test
  public void testConstructors() {
    // Change mock(Client.class) to mock(BaseClient.class)
    var mockClient = mock(BaseClient.class);

    Clients mockClients = new Clients(mockClient);
    List<Client> clientList = List.of(mockClient);

    new Pac4jOptions();
    new Pac4jOptions(mockClients);
    new Pac4jOptions(mockClient);
    new Pac4jOptions(clientList);
    new Pac4jOptions("/custom-callback", mockClient);
    new Pac4jOptions("/custom-callback", clientList);
  }

  @Test
  public void testFromFactoryBranches() {
    // Branch 1: Config is NOT an instance of Pac4jOptions (triggers copy constructor)
    Config baseConfig = new Config();
    Pac4jOptions optionsFromBase = Pac4jOptions.from(baseConfig);
    assertNotNull(optionsFromBase);

    // Branch 2: Config IS an instance of Pac4jOptions (returns same instance)
    Pac4jOptions existingOptions = new Pac4jOptions();
    Pac4jOptions returnedOptions = Pac4jOptions.from(existingOptions);
    assertSame(existingOptions, returnedOptions);
  }

  @Test
  public void testGettersAndSetters() {
    Pac4jOptions options = new Pac4jOptions();

    // Default URL
    assertEquals("/", options.getDefaultUrl());
    options.setDefaultUrl("/home");
    assertEquals("/home", options.getDefaultUrl());
    options.setDefaultUrl(null);
    assertNull(options.getDefaultUrl());

    // Save In Session
    assertNull(options.getSaveInSession());
    options.setSaveInSession(true);
    assertTrue(options.getSaveInSession());

    // Multi Profile
    assertNull(options.getMultiProfile());
    options.setMultiProfile(false);
    assertFalse(options.getMultiProfile());

    // Renew Session
    assertNull(options.getRenewSession());
    options.setRenewSession(true);
    assertTrue(options.getRenewSession());

    // Default Client
    assertNull(options.getDefaultClient());
    options.setDefaultClient("GoogleClient");
    assertEquals("GoogleClient", options.getDefaultClient());

    // Callback Path
    assertEquals("/callback", options.getCallbackPath());
    options.setCallbackPath("/auth");
    assertEquals("/auth", options.getCallbackPath());

    // Logout Path
    assertEquals("/logout", options.getLogoutPath());
    options.setLogoutPath("/signout");
    assertEquals("/signout", options.getLogoutPath());

    // Local Logout
    assertTrue(options.isLocalLogout());
    options.setLocalLogout(false);
    assertFalse(options.isLocalLogout());

    // Central Logout
    assertFalse(options.isCentralLogout());
    options.setCentralLogout(true);
    assertTrue(options.isCentralLogout());

    // Destroy Session
    assertTrue(options.isDestroySession());
    options.setDestroySession(false);
    assertFalse(options.isDestroySession());

    // Cookie SameSite
    assertNull(options.getCookieSameSite());
    options.setCookieSameSite(SameSite.STRICT);
    assertEquals(SameSite.STRICT, options.getCookieSameSite());

    // Force Callback Routes
    assertFalse(options.isForceCallbackRoutes());
    options.setForceCallbackRoutes(true);
    assertTrue(options.isForceCallbackRoutes());

    // Force Logout Routes
    assertFalse(options.isForceLogoutRoutes());
    options.setForceLogoutRoutes(true);
    assertTrue(options.isForceLogoutRoutes());

    // Logout URL Pattern
    assertNull(options.getLogoutUrlPattern());
    options.setLogoutUrlPattern(".*");
    assertEquals(".*", options.getLogoutUrlPattern());

    // Serializer
    assertNotNull(options.getSerializer()); // Defaults to JavaSerializer
    Serializer customSerializer = mock(Serializer.class);
    options.setSerializer(customSerializer);
    assertSame(customSerializer, options.getSerializer());
  }
}
