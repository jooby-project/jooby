/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.jooby.MessageEncoder;
import io.jooby.Route;

public class Issue1822 {

  @Test
  public void routerExistsShouldNotReturnsFalsePositives() {
    String pattern = "/api/*";
    String path = "/";
    Chi router = new Chi();
    router.insert(route("GET", pattern, stringHandler("api")));
    assertFalse(router.exists("GET", path));
    assertTrue(router.exists("GET", "/api/v1"));
  }

  private Route.Handler stringHandler(String foo) {
    return ctx -> foo;
  }

  private Route route(String method, String pattern, Route.Handler handler) {
    return new Route(method, pattern, handler).setEncoder(MessageEncoder.TO_STRING);
  }
}
