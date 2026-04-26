/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

public class ContextSelectorTest {

  @Test
  public void testSingleApplicationSelector() {
    Jooby app = mock(Jooby.class);
    // Selector.create calls single() when list size is 1
    Context.Selector selector = Context.Selector.create(Collections.singletonList(app));

    assertEquals(
        app, selector.select("/any/path"), "Single app selector should always return the app");
    assertEquals(app, selector.select("/"), "Single app selector should always return the app");
  }

  @Test
  public void testMultipleApplicationSelectorMatching() {
    Jooby mainApp = mock(Jooby.class);
    when(mainApp.getContextPath()).thenReturn("/");

    Jooby apiApp = mock(Jooby.class);
    when(apiApp.getContextPath()).thenReturn("/api");

    Jooby adminApp = mock(Jooby.class);
    when(adminApp.getContextPath()).thenReturn("/admin");

    // Selector.create calls multiple() when size > 1
    List<Jooby> apps = Arrays.asList(mainApp, apiApp, adminApp);
    Context.Selector selector = Context.Selector.create(apps);

    // Exact matches / Prefix matches
    assertEquals(apiApp, selector.select("/api"), "Should match /api");
    assertEquals(apiApp, selector.select("/api/v1/users"), "Should match prefix /api");
    assertEquals(adminApp, selector.select("/admin/settings"), "Should match prefix /admin");
  }

  @Test
  public void testMultipleApplicationSelectorFallback() {
    Jooby mainApp = mock(Jooby.class);
    when(mainApp.getContextPath()).thenReturn("/");

    Jooby otherApp = mock(Jooby.class);
    when(otherApp.getContextPath()).thenReturn("/other");

    Context.Selector selector = Context.Selector.create(Arrays.asList(mainApp, otherApp));

    // Fallback to the app defined with "/" context path
    assertEquals(
        mainApp, selector.select("/unknown"), "Should fallback to app with '/' context path");
  }

  @Test
  public void testMultipleApplicationSelectorFallbackToFirst() {
    Jooby app1 = mock(Jooby.class);
    when(app1.getContextPath()).thenReturn("/foo");

    Jooby app2 = mock(Jooby.class);
    when(app2.getContextPath()).thenReturn("/bar");

    // List without a "/" context path
    Context.Selector selector = Context.Selector.create(Arrays.asList(app1, app2));

    // If no app has "/" and no prefix matches, it returns the first app in the list
    assertEquals(
        app1,
        selector.select("/baz"),
        "Should fallback to the first app if no '/' context path is found");
  }

  @Test
  public void testSelectorOrderPrecedence() {
    Jooby app1 = mock(Jooby.class);
    when(app1.getContextPath()).thenReturn("/v1");

    Jooby app2 = mock(Jooby.class);
    when(app2.getContextPath()).thenReturn("/v1/api");

    // Order matters in the provided implementation. It iterates and returns the first match.
    Context.Selector selector = Context.Selector.create(Arrays.asList(app1, app2));

    // Since app1 (/v1) is first, it will consume /v1/api/test because it starts with /v1
    assertEquals(app1, selector.select("/v1/api/test"));
  }
}
