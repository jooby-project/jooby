/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.pac4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.pac4j.core.config.Config;

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
    var options = Pac4jOptions.from(config);
    for (String field : CLONED) {
      var getter =
          Config.class.getMethod(
              "get" + Character.toUpperCase(field.charAt(0)) + field.substring(1));
      assertEquals(values.get(field), getter.invoke(options));
    }
  }
}
