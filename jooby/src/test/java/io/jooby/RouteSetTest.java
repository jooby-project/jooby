/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RouteSetTest {

  private List<Route> routeList;
  private Route route1;
  private Route route2;
  private Route.Set routeSet;

  @BeforeEach
  void setUp() {
    route1 = new Route("GET", "/a", ctx -> "a");
    route2 = new Route("POST", "/b", ctx -> "b");
    routeList = new ArrayList<>(Arrays.asList(route1, route2));
    routeSet = new Route.Set(routeList);
  }

  @Test
  void testGetAndSetRoutes() {
    assertEquals(2, routeSet.getRoutes().size());

    List<Route> newList = Collections.singletonList(new Route("GET", "/c", ctx -> "c"));
    routeSet.setRoutes(newList);
    assertEquals(1, routeSet.getRoutes().size());
    assertEquals("/c", routeSet.getRoutes().get(0).getPattern());
  }

  @Test
  void testProduces() {
    routeSet.produces(MediaType.json, MediaType.xml);

    assertEquals(2, route1.getProduces().size());
    assertTrue(route1.getProduces().contains(MediaType.json));
    assertEquals(2, route2.getProduces().size());

    // Should NOT override if already set
    Route route3 = new Route("GET", "/3", ctx -> "3");
    route3.setProduces(Collections.singletonList(MediaType.html));
    Route.Set set2 = new Route.Set(Collections.singletonList(route3));

    set2.produces(MediaType.json);
    assertEquals(1, route3.getProduces().size());
    assertEquals(MediaType.html, route3.getProduces().get(0));
  }

  @Test
  void testConsumes() {
    routeSet.consumes(MediaType.json);

    assertEquals(MediaType.json, route1.getConsumes().get(0));
    assertEquals(MediaType.json, route2.getConsumes().get(0));
  }

  @Test
  void testAttributes() {
    // Test bulk map
    routeSet.setAttributes(Map.of("attr1", "val1", "attr2", "val2"));
    assertEquals("val1", route1.getAttribute("attr1"));
    assertEquals("val2", route2.getAttribute("attr2"));

    // Test single attribute with putIfAbsent logic
    route1.setAttribute("existing", "original");
    routeSet.setAttribute("existing", "new");
    routeSet.setAttribute("fresh", "value");

    assertEquals("original", route1.getAttribute("existing"));
    assertEquals("value", route1.getAttribute("fresh"));
  }

  @Test
  void testExecutorKey() {
    route1.setExecutorKey("oldKey");
    routeSet.setExecutorKey("newKey");

    assertEquals("oldKey", route1.getExecutorKey());
    assertEquals("newKey", route2.getExecutorKey());
  }

  @Test
  void testTags() {
    routeSet.tags("tag1", "tag2");

    assertEquals(Arrays.asList("tag1", "tag2"), routeSet.getTags());
    assertTrue(route1.getTags().contains("tag1"));
    assertTrue(route2.getTags().contains("tag2"));

    // Check empty tags state
    Route.Set emptySet = new Route.Set(new ArrayList<>());
    assertTrue(emptySet.getTags().isEmpty());
  }

  @Test
  void testSummaryAndDescription() {
    routeSet.summary("General Summary");
    routeSet.description("General Description");

    assertEquals("General Summary", routeSet.getSummary());
    assertEquals("General Description", routeSet.getDescription());

    // Note: Route.Set.setSummary does NOT propagate to individual routes in the current
    // implementation
    // it only stores it in the Set instance for OpenAPI generators.
    assertNull(route1.getSummary());
  }

  @Test
  void testIterator() {
    int count = 0;
    for (Route r : routeSet) {
      assertNotNull(r);
      count++;
    }
    assertEquals(2, count);
  }
}
