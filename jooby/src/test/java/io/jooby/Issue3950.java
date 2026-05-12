/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.typesafe.config.ConfigFactory;

public class Issue3950 {

  @Test
  void shouldLoadSystemPropertiesAfterFreeze() {
    // Not here before
    assertFalse(ConfigFactory.systemProperties().hasPath("auth_client_secret"));
    ConfigFactory.load(ConfigFactory.parseMap(Map.of("bd", Map.of("url", "localhost"))));
    // Not there after
    assertFalse(ConfigFactory.systemProperties().hasPath("auth_client_secret"));

    System.setProperty("auth_client_secret", "xxx");

    var env = Environment.loadEnvironment(new EnvironmentOptions());
    var config = env.getConfig();
    // Sync from environment
    assertEquals("xxx", config.getString("auth_client_secret"));
    // But still not there on config system properties
    assertFalse(ConfigFactory.systemProperties().hasPath("auth_client_secret"));
  }
}
