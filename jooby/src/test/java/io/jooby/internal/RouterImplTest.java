/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.FileNotFoundException;
import java.nio.file.NoSuchFileException;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jooby.*;
import io.jooby.exception.StatusCodeException;

public class RouterImplTest {

  @Test
  @DisplayName("Test errorCode mapping branches (Exact, Superclass, Defaults, and Fallback)")
  public void testErrorCodeBranches() {
    RouterImpl router = new RouterImpl();

    // 1. StatusCodeException branch
    assertEquals(
        StatusCode.UNAUTHORIZED,
        router.errorCode(new StatusCodeException(StatusCode.UNAUTHORIZED, "test")));

    // 2. Custom mapped exact class branch
    router.errorCode(IllegalStateException.class, StatusCode.CONFLICT);
    assertEquals(StatusCode.CONFLICT, router.errorCode(new IllegalStateException()));

    // 3. Custom mapped super class traversal branch
    class CustomIllegalState extends IllegalStateException {}
    assertEquals(
        StatusCode.CONFLICT,
        router.errorCode(new CustomIllegalState()),
        "Should traverse up to find IllegalStateException mapping");

    // 4. Default mappings branch (BAD_REQUEST)
    assertEquals(StatusCode.BAD_REQUEST, router.errorCode(new IllegalArgumentException()));
    assertEquals(StatusCode.BAD_REQUEST, router.errorCode(new NoSuchElementException()));

    // 5. Default mappings branch (NOT_FOUND)
    assertEquals(StatusCode.NOT_FOUND, router.errorCode(new FileNotFoundException()));
    assertEquals(StatusCode.NOT_FOUND, router.errorCode(new NoSuchFileException("test")));

    // 6. Default fallback branch
    assertEquals(StatusCode.SERVER_ERROR, router.errorCode(new RuntimeException()));
  }

  @Test
  @DisplayName("Test setContextPath state validation branches")
  public void testContextPathValidation() {
    RouterImpl router = new RouterImpl();

    // Default branch
    assertEquals("/", router.getContextPath());

    // Valid state branch
    router.setContextPath("/api");
    assertEquals("/api", router.getContextPath());

    // Lock-out branch (Adding a route freezes context path)
    router.get("/route", ctx -> "ok");
    IllegalStateException thrown =
        assertThrows(IllegalStateException.class, () -> router.setContextPath("/v2"));
    assertEquals("Base path must be set before adding any routes.", thrown.getMessage());
  }

  @Test
  @DisplayName("Test domain routing and predicate dispatching branches")
  public void testDomainAndPredicateDispatch() {
    RouterImpl router = new RouterImpl();

    // Mount a route strictly bound to a specific domain
    router.domain(
        "api.jooby.io",
        () -> {
          router.get("/users", ctx -> "users");
        });

    // Match Branch: Domain matches predicate, path matches RouteTree
    Context ctxMatch = mock(Context.class);
    when(ctxMatch.getHost()).thenReturn("api.jooby.io");
    when(ctxMatch.getMethod()).thenReturn(Router.GET);
    when(ctxMatch.getRequestPath()).thenReturn("/users");

    Router.Match hit = router.match(ctxMatch);
    assertTrue(hit.matches());
    assertEquals("/users", hit.route().getPattern());

    // Miss Branch 1: Domain matches, but path doesn't exist
    Context ctxPathMiss = mock(Context.class);
    when(ctxPathMiss.getHost()).thenReturn("api.jooby.io");
    when(ctxPathMiss.getMethod()).thenReturn(Router.GET);
    when(ctxPathMiss.getRequestPath()).thenReturn("/missing");

    assertFalse(router.match(ctxPathMiss).matches());

    // Miss Branch 2: Domain predicate fails (falls through to main Chi tree)
    Context ctxDomainMiss = mock(Context.class);
    when(ctxDomainMiss.getHost()).thenReturn("www.jooby.io");
    when(ctxDomainMiss.getMethod()).thenReturn(Router.GET);
    when(ctxDomainMiss.getRequestPath()).thenReturn("/users");

    assertFalse(router.match(ctxDomainMiss).matches());
  }

  @Test
  @DisplayName("Test Sub-router mounting, route copying, and error handler merging")
  public void testMountSubRouter() {
    RouterImpl parent = new RouterImpl();

    RouterImpl child = new RouterImpl();
    child.get("/child", ctx -> "child");
    child.error((ctx, cause, statusCode) -> {}); // Child error handler

    // Mount branch
    Route.Set mountedRoutes = parent.mount("/api", child);

    assertEquals(1, mountedRoutes.getRoutes().size());
    Route mountedRoute = mountedRoutes.getRoutes().get(0);

    // Path prefixing branch
    assertEquals("/api/child", mountedRoute.getPattern());

    // Verify route was actually copied to parent's routing table
    assertEquals(1, parent.getRoutes().size());
    assertEquals("/api/child", parent.getRoutes().get(0).getPattern());

    // Verify error handler merge
    assertNotNull(parent.getErrorHandler());
  }

  @Test
  @DisplayName("Test unsupported operations (getConfig, getEnvironment, getLocales)")
  public void testUnsupportedOperations() {
    RouterImpl router = new RouterImpl();

    assertThrows(
        UnsupportedOperationException.class,
        router::getConfig,
        "getConfig should throw UnsupportedOperationException");

    assertThrows(
        UnsupportedOperationException.class,
        router::getEnvironment,
        "getEnvironment should throw UnsupportedOperationException");

    assertThrows(
        UnsupportedOperationException.class,
        router::getLocales,
        "getLocales should throw UnsupportedOperationException");
  }

  @Test
  @DisplayName("Test getTmpdir resolves to system temp directory")
  public void testGetTmpdir() {
    RouterImpl router = new RouterImpl();

    java.nio.file.Path expectedPath = java.nio.file.Paths.get(System.getProperty("java.io.tmpdir"));
    assertEquals(
        expectedPath, router.getTmpdir(), "getTmpdir should match java.io.tmpdir system property");
  }

  @Test
  @DisplayName("Test ServiceRegistry delegation (require methods)")
  public void testRequireDelegation() {
    RouterImpl router = new RouterImpl();
    io.jooby.ServiceRegistry registry = router.getServices();

    // 1. Coverage for require(Class)
    registry.put(String.class, "jooby-string");
    assertEquals("jooby-string", router.require(String.class));

    // 2. Coverage for require(ServiceKey)
    io.jooby.ServiceKey<Integer> intKey = io.jooby.ServiceKey.key(Integer.class, "my-int");
    registry.put(intKey, 42);
    assertEquals(42, router.require(intKey));

    // 3. Coverage for require(Reified)
    io.jooby.Reified<java.util.List<String>> reifiedList = io.jooby.Reified.list(String.class);
    java.util.List<String> stringList = java.util.Arrays.asList("a", "b");
    registry.put(ServiceKey.key(reifiedList), stringList);
    assertEquals(stringList, router.require(reifiedList));

    // 4. Coverage for require(Reified, String)
    io.jooby.Reified<java.util.List<Integer>> reifiedNamedList =
        io.jooby.Reified.list(Integer.class);
    java.util.List<Integer> intList = java.util.Arrays.asList(1, 2);
    registry.put(ServiceKey.key(reifiedNamedList, "named-list"), intList);
    assertEquals(intList, router.require(reifiedNamedList, "named-list"));
  }

  @Test
  @DisplayName("Test toString formatting and edge cases")
  public void testToString() {
    RouterImpl router = new RouterImpl();

    // 1. Coverage for empty routes (!buff.isEmpty() == false)
    assertEquals("", router.toString());

    // 2. Coverage for populated routes (padding logic and !buff.isEmpty() == true)
    // Adding routes with different method name lengths to test the padding (size = max length + 1)
    router.get("/users", ctx -> "users");
    router.delete("/users/{id}", ctx -> "deleted");

    // Max method length is DELETE (6) + 1 = 7.
    // GET (3) gets padded with 4 spaces.
    String expected = "  GET    /users\n  DELETE /users/{id}";
    assertEquals(expected, router.toString());

    // 3. Coverage for null routes (if (routes != null) == false)
    router.destroy(); // Sets routes = null internally
    assertEquals("", router.toString());
  }

  @Test
  @DisplayName("Test Pre and Post Dispatch Initializer branches (List Upgrades and Removals)")
  public void testDispatchInitializers() {
    // ==========================================
    // PRE-DISPATCH INITIALIZER COVERAGE
    // ==========================================
    RouterImpl preRouter = new RouterImpl();

    // 1. Add first (hits 'else' branch - single element)
    preRouter.setCurrentUser(ctx -> "user");

    // 2. Add second (hits 'else if != null' branch - upgrades to ContextInitializerList)
    preRouter.setHiddenMethod("_method");

    // 3. Add third (hits 'if instanceof list' branch - adds to existing list)
    RouterOptions proxyOptions = new RouterOptions().setTrustProxy(true);
    preRouter.setRouterOptions(proxyOptions);
    preRouter.initialize(); // Adds PROXY_PEER_ADDRESS to the list

    // 4. Remove from list (hits 'if instanceof list' in remove method)
    proxyOptions.setTrustProxy(false);
    preRouter.initialize(); // Removes PROXY_PEER_ADDRESS from the list

    // 5. Remove single (hits 'else if == initializer' in remove method)
    RouterImpl singlePreRouter = new RouterImpl();
    singlePreRouter.setRouterOptions(new RouterOptions().setTrustProxy(true));
    singlePreRouter.initialize(); // Set single
    singlePreRouter.setRouterOptions(new RouterOptions().setTrustProxy(false));
    singlePreRouter.initialize(); // Remove single (sets to null)

    // ==========================================
    // POST-DISPATCH INITIALIZER COVERAGE
    // ==========================================
    RouterImpl postRouter = new RouterImpl();

    // 1. Add first (hits 'else' branch - single element)
    RouterOptions serviceOptions = new RouterOptions().setContextAsService(true);
    postRouter.setRouterOptions(serviceOptions);
    postRouter.initialize();

    // 2. Add second (hits 'else if != null' branch - upgrades to ContextInitializerList)
    // Calling initialize again triggers addPostDispatchInitializer again
    postRouter.initialize();

    // 3. Add third (hits 'if instanceof list' branch - adds to existing list)
    postRouter.initialize();

    // 4. Remove from list (hits 'if instanceof list' in remove method)
    serviceOptions.setContextAsService(false);
    postRouter.initialize();

    // 5. Remove single (hits 'else if == initializer' in remove method)
    RouterImpl singlePostRouter = new RouterImpl();
    singlePostRouter.setRouterOptions(new RouterOptions().setContextAsService(true));
    singlePostRouter.initialize(); // Set single
    singlePostRouter.setRouterOptions(new RouterOptions().setContextAsService(false));
    singlePostRouter.initialize(); // Remove single (sets to null)
  }
}
