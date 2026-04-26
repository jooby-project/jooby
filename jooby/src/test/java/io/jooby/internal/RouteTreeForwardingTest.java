/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jooby.Route;
import io.jooby.Router;

public class RouteTreeForwardingTest {

  private RouteTree delegate;
  private RouteTreeForwarding forwarding;

  @BeforeEach
  void setUp() {
    delegate = mock(RouteTree.class);
    forwarding = new RouteTreeForwarding(delegate);
  }

  @Test
  public void testInsert() {
    Route route = mock(Route.class);
    forwarding.insert("GET", "/path", route);

    verify(delegate).insert("GET", "/path", route);
  }

  @Test
  public void testExists() {
    when(delegate.exists("POST", "/check")).thenReturn(true);

    boolean result = forwarding.exists("POST", "/check");

    assertTrue(result);
    verify(delegate).exists("POST", "/check");
  }

  @Test
  public void testFind() {
    Router.Match match = mock(Router.Match.class);
    when(delegate.find("GET", "/find")).thenReturn(match);

    Router.Match result = forwarding.find("GET", "/find");

    assertEquals(match, result);
    verify(delegate).find("GET", "/find");
  }

  @Test
  public void testDestroy() {
    forwarding.destroy();

    verify(delegate).destroy();
  }
}
