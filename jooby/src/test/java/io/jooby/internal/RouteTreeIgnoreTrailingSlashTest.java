/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jooby.Router;

public class RouteTreeIgnoreTrailingSlashTest {

  private RouteTree delegate;
  private RouteTreeIgnoreTrailingSlash tree;

  @BeforeEach
  void setUp() {
    delegate = mock(RouteTree.class);
    tree = new RouteTreeIgnoreTrailingSlash(delegate);
  }

  @Test
  @DisplayName("Verify exists() delegates with normalized path (no trailing slash)")
  void testExistsStripsTrailingSlash() {
    String method = "GET";
    String pathWithSlash = "/health/";
    String pathWithoutSlash = "/health";

    // Setup: the delegate should receive the path without the slash
    when(delegate.exists(eq(method), eq(pathWithoutSlash))).thenReturn(true);

    boolean result = tree.exists(method, pathWithSlash);

    assertTrue(result, "exists() should have stripped the trailing slash before delegation.");
  }

  @Test
  @DisplayName("Verify find() delegates with normalized path (no trailing slash)")
  void testFindStripsTrailingSlash() {
    String method = "POST";
    String pathWithSlash = "/api/data/";
    String pathWithoutSlash = "/api/data";

    Router.Match mockMatch = mock(Router.Match.class);

    // Setup: the delegate should receive the path without the slash
    when(delegate.find(eq(method), eq(pathWithoutSlash))).thenReturn(mockMatch);

    Router.Match result = tree.find(method, pathWithSlash);

    assertEquals(
        mockMatch, result, "find() should have stripped the trailing slash before delegation.");
  }

  @Test
  @DisplayName("Verify no-op when path has no trailing slash")
  void testNoTrailingSlash() {
    String method = "GET";
    String path = "/users";

    when(delegate.exists(eq(method), eq(path))).thenReturn(true);

    assertTrue(tree.exists(method, path), "Should work correctly even if no slash is present.");
  }
}
