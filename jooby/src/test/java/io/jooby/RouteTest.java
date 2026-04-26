/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jooby.annotation.Transactional;

public class RouteTest {

  private Route.Handler handler;

  @BeforeEach
  void setUp() {
    handler = mock(Route.Handler.class);
  }

  @Test
  void testConstructorAndBasics() {
    Route route = new Route("get", "/path", handler);

    assertEquals("GET", route.getMethod());
    assertEquals("/path", route.getPattern());
    assertEquals(handler, route.getHandler());
    assertNotNull(route.getLocation());
    // Verify toString format
    assertEquals("GET /path", route.toString());
  }

  @Test
  void testMetadataCollections() {
    Route route = new Route("GET", "/", handler);

    // Path Keys
    route.setPathKeys(List.of("id"));
    assertEquals(List.of("id"), route.getPathKeys());

    // Produces
    route.produces(MediaType.json);
    route.setProduces(List.of(MediaType.xml));
    assertEquals(2, route.getProduces().size());

    // Consumes
    route.consumes(MediaType.json);
    route.setConsumes(List.of(MediaType.xml));
    assertEquals(2, route.getConsumes().size());

    // Tags
    route.tags("tag1");
    route.addTag("tag2");
    route.setTags(List.of("tag3"));
    assertEquals(3, route.getTags().size());
  }

  @Test
  void testAttributes() {
    Route route = new Route("GET", "/", handler);

    route.setAttribute("foo", "bar");
    assertEquals("bar", route.getAttribute("foo"));

    route.setAttributes(Map.of("key", "val"));
    assertEquals("val", route.getAttribute("key"));
    assertEquals(2, route.getAttributes().size());
  }

  @Test
  void testPipelineComputation() throws Exception {
    Route route = new Route("GET", "/", handler);

    // 1. Default pipeline is just the handler
    assertEquals(handler, route.getPipeline());

    // 2. Add filter
    Route.Filter filter = mock(Route.Filter.class);
    // When the filter is applied to the handler, it returns a new wrapped handler
    Route.Handler filteredHandler = mock(Route.Handler.class);
    when(filter.then(handler)).thenReturn(filteredHandler);

    route.setFilter(filter);

    // IMPORTANT: Clear the cached pipeline to force re-computation
    route.setPipeline(null);

    assertEquals(filteredHandler, route.getPipeline());

    // 3. Add After
    Route.After after = mock(Route.After.class);
    Route.Handler finalHandler = mock(Route.Handler.class);
    // The previous 'filteredHandler' is now the 'next' in the chain for 'then(after)'
    when(filteredHandler.then(after)).thenReturn(finalHandler);

    route.setAfter(after);

    // Clear the cached pipeline again
    route.setPipeline(null);

    assertEquals(finalHandler, route.getPipeline());
  }

  @Test
  void testNonBlocking() {
    Route route = new Route("GET", "/", handler);
    assertFalse(route.isNonBlockingSet());

    route.setNonBlocking(true);
    assertTrue(route.isNonBlocking());
    assertTrue(route.isNonBlockingSet());
  }

  @Test
  void testHttpMethodsEnabled() {
    Route route = new Route("GET", "/", handler);

    assertFalse(route.isHttpOptions());
    route.setHttpOptions(true);
    assertTrue(route.isHttpOptions());

    assertFalse(route.isHttpTrace());
    route.setHttpTrace(true);
    assertTrue(route.isHttpTrace());

    assertFalse(route.isHttpHead());
    route.setHttpHead(true);
    assertTrue(route.isHttpHead());

    // Toggle off
    route.setHttpOptions(false);
    assertFalse(route.isHttpOptions());
  }

  @Test
  void testTransactional() {
    Route route = new Route("GET", "/", handler);

    // Default
    assertTrue(route.isTransactional(true));
    assertFalse(route.isTransactional(false));

    // Explicitly set
    route.setAttribute(Transactional.ATTRIBUTE, true);
    assertTrue(route.isTransactional(false));

    route.setAttribute(Transactional.ATTRIBUTE, "not-a-boolean");
    assertThrows(RuntimeException.class, () -> route.isTransactional(true));
  }

  @Test
  void testExecutorAndDocumentation() {
    Route route = new Route("GET", "/", handler);

    route.setExecutorKey("worker");
    assertEquals("worker", route.getExecutorKey());

    route.summary("sum").description("desc");
    assertEquals("sum", route.getSummary());
    assertEquals("desc", route.getDescription());
  }

  @Test
  void testDecoders() {
    Route route = new Route("GET", "/", handler);
    MessageDecoder decoder = mock(MessageDecoder.class);

    route.setDecoders(Map.of(MediaType.json.getValue(), decoder));

    assertEquals(decoder, route.decoder(MediaType.json));
    assertEquals(MessageDecoder.UNSUPPORTED_MEDIA_TYPE, route.decoder(MediaType.xml));
    assertEquals(1, route.getDecoders().size());
  }

  @Test
  void testReverse() {
    // This assumes Router.reverse logic is accessible
    Route route = new Route("GET", "/user/{id}", handler);
    // Note: Reversing usually delegates to Router, so we check it doesn't crash
    assertNotNull(route.reverse(Map.of("id", 123)));
    assertNotNull(route.reverse(123));
  }
}
