/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.jooby.annotation.Transactional;
import io.jooby.exception.BadRequestException;
import io.jooby.exception.MethodNotAllowedException;
import io.jooby.exception.NotAcceptableException;
import io.jooby.exception.NotFoundException;
import io.jooby.exception.StatusCodeException;
import io.jooby.exception.UnsupportedMediaType;
import io.jooby.value.Value;

class RouteHandlerTest {

  private Context ctx;
  private Router router;

  @BeforeEach
  void setUp() {
    ctx = mock(Context.class);
    router = mock(Router.class);
    lenient().when(ctx.getRouter()).thenReturn(router);
  }

  // --- Basic Routing Metadata & Accessors ---

  @Test
  @DisplayName("Test basic accessors and Route location")
  void basicAccessors() {
    Route.Handler handler = mock(Route.Handler.class);
    Route route = new Route("GET", "/api", handler);

    assertEquals("GET", route.getMethod());
    assertEquals("/api", route.getPattern());
    assertEquals(handler, route.getHandler());
    assertEquals("GET /api", route.toString());

    // Location is captured via StackWalker
    assertNotNull(route.getLocation());
    assertNotNull(route.getLocation().filename());
    assertTrue(route.getLocation().line() > 0);

    route.setPathKeys(Arrays.asList("id"));
    assertEquals(Collections.singletonList("id"), route.getPathKeys());

    route.setEncoder(MessageEncoder.TO_STRING);
    assertEquals(MessageEncoder.TO_STRING, route.getEncoder());

    route.setNonBlocking(true);
    assertTrue(route.isNonBlocking());
    assertTrue(route.isNonBlockingSet());

    route.setSummary("Summary").setDescription("Desc");
    assertEquals("Summary", route.getSummary());
    assertEquals("Desc", route.getDescription());

    route.tags("api", "v1");
    assertEquals(Arrays.asList("api", "v1"), route.getTags());
    route.addTag("v2");
    assertTrue(route.getTags().contains("v2"));

    route.setExecutorKey("worker");
    assertEquals("worker", route.getExecutorKey());
  }

  @Test
  @DisplayName("Test Produces and Consumes")
  void producesAndConsumes() {
    Route route = new Route("GET", "/", mock(Route.Handler.class));
    assertTrue(route.getProduces().isEmpty());
    assertTrue(route.getConsumes().isEmpty());

    route.produces(MediaType.json);
    route.setProduces(Arrays.asList(MediaType.html));
    assertEquals(Arrays.asList(MediaType.json, MediaType.html), route.getProduces());

    route.consumes(MediaType.json);
    route.setConsumes(Arrays.asList(MediaType.xml));
    assertEquals(Arrays.asList(MediaType.json, MediaType.xml), route.getConsumes());
  }

  @Test
  @DisplayName("Test Attributes and Transactional")
  void attributes() {
    Route route = new Route("GET", "/", mock(Route.Handler.class));

    route.setAttribute("key", "val");
    assertEquals("val", route.getAttribute("key"));

    route.setAttributes(Map.of("key2", "val2"));
    assertEquals("val2", route.getAttribute("key2"));

    // Transactional logic
    assertTrue(route.isTransactional(true)); // Default fallback

    route.setAttribute(Transactional.ATTRIBUTE, false);
    assertFalse(route.isTransactional(true));

    route.setAttribute(Transactional.ATTRIBUTE, "InvalidType");
    assertThrows(RuntimeException.class, () -> route.isTransactional(true));
  }

  @Test
  @DisplayName("Test Decoders and HTTP Methods")
  void decodersAndHttpMethods() {
    Route route = new Route("GET", "/", mock(Route.Handler.class));

    // Decoders
    assertEquals(MessageDecoder.UNSUPPORTED_MEDIA_TYPE, route.decoder(MediaType.json));
    MessageDecoder customDecoder = mock(MessageDecoder.class);
    route.setDecoders(Map.of(MediaType.JSON, customDecoder));
    assertEquals(customDecoder, route.decoder(MediaType.json));

    // HTTP Methods
    assertFalse(route.isHttpOptions());
    route.setHttpOptions(true);
    assertTrue(route.isHttpOptions());
    route.setHttpOptions(false);
    assertFalse(route.isHttpOptions());

    assertFalse(route.isHttpTrace());
    route.setHttpTrace(true);
    assertTrue(route.isHttpTrace());

    assertFalse(route.isHttpHead());
    route.setHttpHead(true);
    assertTrue(route.isHttpHead());
  }

  @Test
  @DisplayName("Test Reverse Routing")
  void reverseRouting() {
    Route route = new Route("GET", "/{id}", mock(Route.Handler.class));
    // Since Router.reverse is static, we just verify it doesn't crash and returns the template
    // Jooby's static router reverse logic returns the formatted string
    assertEquals("/1", route.reverse("1"));
    assertEquals("/1", route.reverse(Map.of("id", "1")));
  }

  // --- Pipeline, Filters, Before, After, and Handler Chaining ---

  @Test
  @DisplayName("Test Filter Chaining (ThenFilter, ThenHandler)")
  void filterChaining() throws Exception {
    Route.Filter f1 = next -> ctx -> "F1+" + next.apply(ctx);
    Route.Filter f2 = next -> ctx -> "F2+" + next.apply(ctx);
    Route.Handler handler = ctx -> "H";

    // Standard filters work because they are real lambda implementations
    Route.Filter chainedFilter = f1.then(f2);
    Route.Handler chainedHandler = chainedFilter.then(handler);

    assertEquals("F1+F2+H", chainedHandler.apply(ctx));

    // --- Aware setRoute propagation ---
    Route mockRoute = mock(Route.class);

    // FIX: Use CALLS_REAL_METHODS so the default .then(...) method is actually executed
    Route.Filter mockAwareFilter =
        mock(Route.Filter.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
    Route.Handler mockAwareHandler =
        mock(Route.Handler.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));

    // Now this will return a real ThenHandler record instead of null
    Route.Handler combined = mockAwareFilter.then(mockAwareHandler);

    assertNotNull(combined, "Combined handler should not be null when default methods are called");

    combined.setRoute(mockRoute);

    // Verify that the call propagated through the ThenHandler to the underlying filter/handler
    verify(mockAwareFilter).setRoute(mockRoute);
    verify(mockAwareHandler).setRoute(mockRoute);
  }

  @Test
  @DisplayName("Test Before Filter Chaining")
  void beforeFilterChaining() throws Exception {
    List<String> events = new ArrayList<>();
    Route.Before b1 = ctx -> events.add("B1");
    Route.Before b2 = ctx -> events.add("B2");
    Route.Handler h =
        ctx -> {
          events.add("H");
          return "Result";
        };

    when(ctx.isResponseStarted()).thenReturn(false);

    // Before then Before
    Route.Before chainedBefore = b1.then(b2);
    chainedBefore.apply(ctx);
    assertEquals(Arrays.asList("B1", "B2"), events);

    events.clear();

    // Before then Handler
    Route.Handler chainedHandler = chainedBefore.then(h);
    assertEquals("Result", chainedHandler.apply(ctx));
    assertEquals(Arrays.asList("B1", "B2", "H"), events);

    // Abort chain if response started
    events.clear();
    when(ctx.isResponseStarted()).thenReturn(true);
    Object result = b1.then(h).apply(ctx);
    assertEquals(ctx, result); // Returns ctx if response started
    assertEquals(Collections.singletonList("B1"), events); // H is skipped
  }

  @Test
  @DisplayName("Test After Filter Chaining")
  void afterFilterChaining() throws Exception {
    List<String> events = new ArrayList<>();
    Route.After a1 = (ctx, result, failure) -> events.add("A1");
    Route.After a2 = (ctx, result, failure) -> events.add("A2");

    Route.After chained = a1.then(a2);
    chained.apply(ctx, "Result", null);

    // Notice the implementation runs 'next.apply' then 'this.apply'
    // So a1.then(a2) -> runs a2 then a1
    assertEquals(Arrays.asList("A2", "A1"), events);
  }

  @Test
  @DisplayName("Test Handler.then(After) Logic Branches")
  void handlerThenAfterBranches() throws Exception {
    Route.After after = mock(Route.After.class);

    // 1. Happy Path (No exception, Response not started)
    Route.Handler hHappy = ctx -> "Happy";
    Route.Handler pHappy = hHappy.then(after);
    when(ctx.isResponseStarted()).thenReturn(false);
    assertEquals("Happy", pHappy.apply(ctx));
    verify(after).apply(ctx, "Happy", null);

    // 2. Exception in Handler
    Route.Handler hException =
        ctx -> {
          throw new RuntimeException("Crash");
        };
    Route.Handler pException = hException.then(after);
    when(ctx.isResponseStarted()).thenReturn(false);
    when(router.errorCode(any())).thenReturn(StatusCode.SERVER_ERROR);

    assertThrows(RuntimeException.class, () -> pException.apply(ctx));
    verify(ctx).setResponseCode(StatusCode.SERVER_ERROR);
    verify(after).apply(eq(ctx), isNull(), any(RuntimeException.class));

    // 3. Response Started Path (Wraps context in ReadOnly)
    Route.Handler hStarted = ctx -> "Started";
    Route.Handler pStarted = hStarted.then(after);
    when(ctx.isResponseStarted()).thenReturn(true);

    // FIX: When response is started, it returns Context.readOnly(ctx) instead of the value
    Object startedResult = pStarted.apply(ctx);
    assertTrue(startedResult instanceof Context, "Should return a Context (readOnly wrapper)");
    verify(after).apply(any(Context.class), eq("Started"), isNull());

    // 4. Exception in Handler AND Exception in After (Suppressed exception)
    RuntimeException handlerEx = new RuntimeException("Handler Error");
    RuntimeException afterEx = new RuntimeException("After Error");
    Route.Handler hDoubleCrash =
        ctx -> {
          throw handlerEx;
        };
    Route.After aCrash =
        (c, r, f) -> {
          throw afterEx;
        };
    Route.Handler pDoubleCrash = hDoubleCrash.then(aCrash);

    when(ctx.isResponseStarted()).thenReturn(false);
    RuntimeException caught = assertThrows(RuntimeException.class, () -> pDoubleCrash.apply(ctx));
    assertEquals("Handler Error", caught.getMessage());
    assertEquals("After Error", caught.getSuppressed()[0].getMessage());

    // 5. Exception but Response already started -> Returns ctx instead of propagating
    when(ctx.isResponseStarted()).thenReturn(true);
    Object exceptionStartedResult = pException.apply(ctx);
    assertTrue(
        exceptionStartedResult instanceof Context,
        "Should return Context if exception thrown but response started");
  }

  @Test
  @DisplayName("Test Pipeline Computation")
  void computePipeline() throws Exception {
    Route.Handler h = ctx -> "Result";
    Route route = new Route("GET", "/", h);

    // No filters -> pipeline is handler
    assertEquals(h, route.getPipeline());

    route.setPipeline(null); // reset

    // Filter + After
    Route.Filter f = next -> ctx -> "F+" + next.apply(ctx);
    Route.After a = mock(Route.After.class);

    route.setFilter(f).setAfter(a);
    Route.Handler pipeline = route.getPipeline();

    assertEquals("F+Result", pipeline.apply(ctx));
    verify(a).apply(ctx, "F+Result", null);
  }

  // --- Static Handlers ---

  @Test
  @DisplayName("Test NOT_FOUND handler")
  void testNotFound() throws Exception {
    when(ctx.getRequestPath()).thenReturn("/missing");
    Route.NOT_FOUND.apply(ctx);

    ArgumentCaptor<NotFoundException> captor = ArgumentCaptor.forClass(NotFoundException.class);
    verify(ctx).sendError(captor.capture());
    assertEquals("/missing", captor.getValue().getMessage());
  }

  @Test
  @DisplayName("Test METHOD_NOT_ALLOWED handler")
  void testMethodNotAllowed() throws Exception {
    // OPTIONS request
    when(ctx.getMethod()).thenReturn(Router.OPTIONS);
    Route.METHOD_NOT_ALLOWED.apply(ctx);
    verify(ctx).setResetHeadersOnError(false);
    verify(ctx).send(StatusCode.OK);

    // POST request (Not options)
    when(ctx.getMethod()).thenReturn("POST");
    when(ctx.getResponseHeader("Allow")).thenReturn("GET,POST");
    Route.METHOD_NOT_ALLOWED.apply(ctx);

    ArgumentCaptor<MethodNotAllowedException> captor =
        ArgumentCaptor.forClass(MethodNotAllowedException.class);
    verify(ctx).sendError(captor.capture());
    assertEquals(Arrays.asList("GET", "POST"), captor.getValue().getAllow());
  }

  @Test
  @DisplayName("Test FORM_DECODER_HANDLER")
  void testFormDecoderHandler() throws Exception {
    Map<String, Object> attributes = new HashMap<>();
    when(ctx.getAttributes()).thenReturn(attributes);

    // Generic decode fail
    Route.FORM_DECODER_HANDLER.apply(ctx);
    verify(ctx).sendError(any(BadRequestException.class));

    // Too many fields
    attributes.put("__too_many_fields", new IllegalStateException("Max exceeded"));
    Route.FORM_DECODER_HANDLER.apply(ctx);
    verify(ctx, times(2)).sendError(any(BadRequestException.class));
  }

  @Test
  @DisplayName("Test REQUEST_ENTITY_TOO_LARGE")
  void testRequestEntityTooLarge() throws Exception {
    when(ctx.setResponseCode(any(StatusCode.class))).thenReturn(ctx);
    Route.REQUEST_ENTITY_TOO_LARGE.apply(ctx);
    verify(ctx).setResponseCode(StatusCode.REQUEST_ENTITY_TOO_LARGE);
    verify(ctx).sendError(any(StatusCodeException.class));
  }

  @Test
  @DisplayName("Test ACCEPT Filter")
  void testAcceptFilter() throws Exception {
    Route route = new Route("GET", "/", ctx -> "ok").produces(MediaType.json);
    when(ctx.getRoute()).thenReturn(route);

    // Match found
    when(ctx.accept(route.getProduces())).thenReturn(MediaType.json);
    Route.ACCEPT.apply(ctx);
    verify(ctx).setDefaultResponseType(MediaType.json);

    // No match found
    when(ctx.accept(route.getProduces())).thenReturn(null);
    var mockAcceptHeader = mock(Value.class);
    when(mockAcceptHeader.valueOrNull()).thenReturn("text/html");
    when(ctx.header(Context.ACCEPT)).thenReturn(mockAcceptHeader);

    assertThrows(NotAcceptableException.class, () -> Route.ACCEPT.apply(ctx));
  }

  @Test
  @DisplayName("Test SUPPORT_MEDIA_TYPE Filter")
  void testSupportMediaType() throws Exception {
    Route route = new Route("GET", "/", ctx -> "ok").consumes(MediaType.json);
    when(ctx.getRoute()).thenReturn(route);

    // Preflight -> Do nothing
    when(ctx.isPreflight()).thenReturn(true);
    Route.SUPPORT_MEDIA_TYPE.apply(ctx); // Does not throw

    // Missing Content-Type
    when(ctx.isPreflight()).thenReturn(false);
    when(ctx.getRequestType()).thenReturn(null);
    assertThrows(UnsupportedMediaType.class, () -> Route.SUPPORT_MEDIA_TYPE.apply(ctx));

    // Mismatched Content-Type
    when(ctx.getRequestType()).thenReturn(MediaType.html);
    assertThrows(UnsupportedMediaType.class, () -> Route.SUPPORT_MEDIA_TYPE.apply(ctx));

    // Matched Content-Type
    when(ctx.getRequestType()).thenReturn(MediaType.json);
    Route.SUPPORT_MEDIA_TYPE.apply(ctx); // Does not throw
  }

  @Test
  @DisplayName("Test FAVICON Handler")
  void testFavicon() throws Exception {
    Route.FAVICON.apply(ctx);
    verify(ctx).send(StatusCode.NOT_FOUND);
  }

  // --- MVC Method ---

  public static class DummyController {
    public void validMethod() {}
  }

  @Test
  @DisplayName("Test MvcMethod reflection and MethodHandles")
  void testMvcMethod() throws Exception {
    Route.MvcMethod mvc = new Route.MvcMethod(DummyController.class, "validMethod", void.class);
    assertNotNull(mvc.toMethod());
    assertNotNull(mvc.toMethodHandle());

    Route.MvcMethod invalidMvc = new Route.MvcMethod(DummyController.class, "missing", void.class);
    assertThrows(NoSuchMethodException.class, invalidMvc::toMethod);

    Route route = new Route("GET", "/", ctx -> "ok");
    route.mvcMethod(mvc);
    assertEquals(mvc, route.getMvcMethod());
  }

  // --- Route.Set (Bulk Operations) ---

  @Test
  @DisplayName("Test Route.Set bulk setters")
  void testRouteSet() {
    Route r1 = new Route("GET", "/1", ctx -> "1");
    Route r2 = new Route("GET", "/2", ctx -> "2");

    Route.Set routeSet = new Route.Set(Arrays.asList(r1, r2));

    // Produces & Consumes
    routeSet.produces(MediaType.json);
    routeSet.consumes(MediaType.html);
    assertEquals(Collections.singletonList(MediaType.json), r1.getProduces());
    assertEquals(Collections.singletonList(MediaType.html), r2.getConsumes());

    // Attributes
    routeSet.setAttribute("k1", "v1");
    routeSet.setAttributes(Map.of("k2", "v2"));
    assertEquals("v1", r1.getAttribute("k1"));
    assertEquals("v2", r2.getAttribute("k2"));

    // Executor
    routeSet.setExecutorKey("pool");
    assertEquals("pool", r1.getExecutorKey());

    // Tags
    assertTrue(routeSet.getTags().isEmpty());
    routeSet.tags("t1");
    assertEquals(Collections.singletonList("t1"), r2.getTags());
    assertEquals(Collections.singletonList("t1"), routeSet.getTags());

    // Summary & Description
    routeSet.summary("Sum").description("Desc");
    assertEquals("Sum", routeSet.getSummary());
    assertEquals("Desc", routeSet.getDescription());

    // Iterable
    List<Route> collected = new ArrayList<>();
    routeSet.forEach(collected::add);
    assertEquals(2, collected.size());

    // Test getRoutes/setRoutes
    assertEquals(Arrays.asList(r1, r2), routeSet.getRoutes());
    routeSet.setRoutes(Collections.singletonList(r1));
    assertEquals(1, routeSet.getRoutes().size());
  }
}
