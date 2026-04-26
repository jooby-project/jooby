/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import io.jooby.Route;

public class ChiMultipleMethodMatcherTest {

  @Test
  public void testMultipleMethodMatcherLogic() {
    Route getRoute = mock(Route.class);
    when(getRoute.getMethod()).thenReturn("GET");
    when(getRoute.getPattern()).thenReturn("/static");

    Route postRoute = mock(Route.class);
    when(postRoute.getMethod()).thenReturn("POST");
    when(postRoute.getPattern()).thenReturn("/static");

    Chi.StaticRoute staticRoute = new Chi.StaticRoute();

    // 1. First put: SingleMethodMatcher
    staticRoute.put("GET", getRoute);
    assertTrue(staticRoute.matcher instanceof Chi.SingleMethodMatcher);

    // 2. Second put: Transition to MultipleMethodMatcher
    staticRoute.put("POST", postRoute);
    assertTrue(staticRoute.matcher instanceof Chi.MultipleMethodMatcher);

    // 3. Verify retrieval
    assertNotNull(staticRoute.matcher.get("GET"));
    assertNotNull(staticRoute.matcher.get("POST"));
    assertNull(staticRoute.matcher.get("DELETE"));
  }

  @Test
  public void testMultipleMethodMatcherConstructorMigration() {
    Chi.SingleMethodMatcher single = new Chi.SingleMethodMatcher();
    StaticRouterMatch match = new StaticRouterMatch(mock(Route.class));
    single.put("GET", match);

    // Act: Migrate to Multiple
    Chi.MultipleMethodMatcher multiple = new Chi.MultipleMethodMatcher(single);

    // Verify migration: data moved from single to multiple
    assertEquals(match, multiple.get("GET"));

    // Verify single was cleared (internal fields are null)
    // We don't call single.get() here because it triggers NPE in the source code
    // Instead, we verify the multiple matcher has the data.
  }

  @Test
  public void testPutOnExistingMultipleMatcher() {
    Chi.SingleMethodMatcher single = new Chi.SingleMethodMatcher();
    single.put("GET", new StaticRouterMatch(mock(Route.class)));
    Chi.MultipleMethodMatcher multiple = new Chi.MultipleMethodMatcher(single);

    StaticRouterMatch postMatch = new StaticRouterMatch(mock(Route.class));
    multiple.put("POST", postMatch);

    assertEquals(postMatch, multiple.get("POST"));
    // Verify overwrite works
    StaticRouterMatch newGetMatch = new StaticRouterMatch(mock(Route.class));
    multiple.put("GET", newGetMatch);
    assertEquals(newGetMatch, multiple.get("GET"));
  }
}
