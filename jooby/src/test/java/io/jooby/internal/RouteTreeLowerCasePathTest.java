/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jooby.Router;

public class RouteTreeLowerCasePathTest {

  private RouteTree delegate;
  private RouteTreeLowerCasePath lowerCaseTree;

  @BeforeEach
  void setUp() {
    delegate = mock(RouteTree.class);
    lowerCaseTree = new RouteTreeLowerCasePath(delegate);
  }

  @Test
  @DisplayName("Verify exists() normalizes mixed-case path to lowercase")
  void testExistsLowerCasesPath() {
    String method = "GET";
    String mixedCasePath = "/User/Profile";
    String lowerCasePath = "/user/profile";

    // Configure delegate to return true ONLY for the lowercased version
    when(delegate.exists(method, lowerCasePath)).thenReturn(true);

    boolean result = lowerCaseTree.exists(method, mixedCasePath);

    assertTrue(result, "The path should have been lowercased before calling the delegate.");
  }

  @Test
  @DisplayName("Verify find() normalizes mixed-case path to lowercase")
  void testFindLowerCasesPath() {
    String method = "POST";
    String mixedCasePath = "/API/v1/Login";
    String lowerCasePath = "/api/v1/login";

    Router.Match mockMatch = mock(Router.Match.class);

    // Configure delegate to return the match ONLY for the lowercased version
    when(delegate.find(method, lowerCasePath)).thenReturn(mockMatch);

    Router.Match result = lowerCaseTree.find(method, mixedCasePath);

    assertEquals(
        mockMatch, result, "The path should have been lowercased before calling the delegate.");
  }
}
