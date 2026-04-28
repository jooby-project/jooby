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

public class RouteTreeNormPathTest {

  private RouteTree delegate;
  private RouteTreeNormPath tree;

  @BeforeEach
  void setUp() {
    delegate = mock(RouteTree.class);
    tree = new RouteTreeNormPath(delegate);
  }

  @Test
  @DisplayName("Verify exists() delegates with normalized path (handles double slashes)")
  void testExistsNormalizesPath() {
    String method = "GET";
    // Router.normalizePath typically converts // into /
    String messyPath = "//user//profile";
    String cleanPath = "/user/profile";

    // Configure delegate to return true ONLY for the normalized version
    when(delegate.exists(eq(method), eq(cleanPath))).thenReturn(true);

    boolean result = tree.exists(method, messyPath);

    assertTrue(result, "exists() should have normalized the path before delegation.");
  }

  @Test
  @DisplayName("Verify find() delegates with normalized path")
  void testFindNormalizesPath() {
    String method = "POST";
    String messyPath = "/api/v1//login";
    String cleanPath = "/api/v1/login";

    Router.Match mockMatch = mock(Router.Match.class);

    // Configure delegate to return the match ONLY for the normalized version
    when(delegate.find(eq(method), eq(cleanPath))).thenReturn(mockMatch);

    Router.Match result = tree.find(method, messyPath);

    assertEquals(mockMatch, result, "find() should have normalized the path before delegation.");
  }

  @Test
  @DisplayName("Verify no-op when path is already normalized")
  void testAlreadyNormalized() {
    String method = "GET";
    String path = "/already/clean";

    when(delegate.exists(eq(method), eq(path))).thenReturn(true);

    assertTrue(tree.exists(method, path), "Should function correctly for already clean paths.");
  }
}
