/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jooby.Context;
import io.jooby.ErrorHandler;
import io.jooby.Jooby;
import io.jooby.Route;
import io.jooby.Route.Handler;
import io.jooby.Router;
import io.jooby.StatusCode;
import io.jooby.WebSocket;

class MockRouterTest {

  private Jooby app;
  private MockRouter router;

  @BeforeEach
  void setUp() {
    app = mock(Jooby.class);
    when(app.problemDetailsIsEnabled()).thenReturn(false);
    router = new MockRouter(app);
  }

  // --- CONSTRUCTOR TESTS ---

  @Test
  void testConstructorWithProblemDetailsEnabled() {
    Jooby appWithProblems = mock(Jooby.class);
    when(appWithProblems.problemDetailsIsEnabled()).thenReturn(true);
    when(appWithProblems.getConfig()).thenReturn(com.typesafe.config.ConfigFactory.empty());

    new MockRouter(appWithProblems);

    verify(appWithProblems).error(any(ErrorHandler.class));
  }

  // --- SETTER & GETTER TESTS ---

  @Test
  void testSetAndGetWorker() {
    Executor worker = Executors.newSingleThreadExecutor();
    assertEquals(router, router.setWorker(worker));
    assertEquals(worker, router.getWorker());
  }

  @Test
  void testSetSession() {
    MockSession session = new MockSession();
    session.put("key", "value");

    assertEquals(router, router.setSession(session));

    // We can verify this worked by calling a route and checking if the injected session has the
    // value
    Route mockRoute = mock(Route.class);
    when(mockRoute.getMethod()).thenReturn(Router.GET);
    when(mockRoute.getHandler()).thenReturn(ctx -> ctx.session().get("key").value());

    Router.Match match = mock(Router.Match.class);
    when(match.route()).thenReturn(mockRoute);
    when(match.pathMap()).thenReturn(new HashMap<>());
    when(app.match(any(Context.class))).thenReturn(match);

    MockValue result = router.get("/");
    assertEquals("value", result.value());
  }

  // --- HTTP METHOD TESTS ---

  @Test
  void testGet() throws Exception {
    setupMockRoute(Router.GET, "GetResult");

    assertEquals("GetResult", router.get("/").value());
    assertEquals("GetResult", router.get("/", mock(Context.class)).value());
    assertEquals("GetResult", router.get("/", ctx -> {}).value());
    assertEquals("GetResult", router.get("/", new MockContext(), ctx -> {}).value());
  }

  @Test
  void testPost() throws Exception {
    setupMockRoute(Router.POST, "PostResult");

    assertEquals("PostResult", router.post("/").value());
    assertEquals("PostResult", router.post("/", mock(Context.class)).value());
    assertEquals("PostResult", router.post("/", ctx -> {}).value());
    assertEquals("PostResult", router.post("/", new MockContext(), ctx -> {}).value());
  }

  @Test
  void testPut() throws Exception {
    setupMockRoute(Router.PUT, "PutResult");

    assertEquals("PutResult", router.put("/").value());
    assertEquals("PutResult", router.put("/", mock(Context.class)).value());
    assertEquals("PutResult", router.put("/", ctx -> {}).value());
    assertEquals("PutResult", router.put("/", new MockContext(), ctx -> {}).value());
  }

  @Test
  void testPatch() throws Exception {
    setupMockRoute(Router.PATCH, "PatchResult");

    assertEquals("PatchResult", router.patch("/").value());
    assertEquals("PatchResult", router.patch("/", mock(Context.class)).value());
    assertEquals("PatchResult", router.patch("/", ctx -> {}).value());
    assertEquals("PatchResult", router.patch("/", new MockContext(), ctx -> {}).value());
  }

  @Test
  void testDelete() throws Exception {
    setupMockRoute(Router.DELETE, "DeleteResult");

    assertEquals("DeleteResult", router.delete("/").value());
    assertEquals("DeleteResult", router.delete("/", mock(Context.class)).value());
    assertEquals("DeleteResult", router.delete("/", ctx -> {}).value());
    assertEquals("DeleteResult", router.delete("/", new MockContext(), ctx -> {}).value());
  }

  // --- WEBSOCKET TESTS ---

  @Test
  void testWs() {
    Route wsRoute = mock(Route.class);
    when(wsRoute.getMethod()).thenReturn(Router.WS);

    WebSocket.Handler wsHandler = mock(WebSocket.Handler.class);
    WebSocket.Initializer wsInit = mock(WebSocket.Initializer.class);
    when(wsHandler.getInitializer()).thenReturn(wsInit);
    when(wsRoute.getHandler()).thenReturn(wsHandler);

    Router.Match match = mock(Router.Match.class);
    when(match.route()).thenReturn(wsRoute);
    when(match.pathMap()).thenReturn(new HashMap<>());
    when(app.match(any(Context.class))).thenReturn(match);

    Consumer<MockWebSocketClient> callback = mock(Consumer.class);

    MockWebSocketClient client = router.ws("/ws", callback);

    assertNotNull(client);
    verify(callback, times(1)).accept(client);
  }

  @Test
  void testWsThrowsIllegalArgumentExceptionIfRouteIsNotWs() throws Exception {
    setupMockRoute(Router.GET, "NotAWS");

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> router.ws("/ws", client -> {}));

    assertTrue(ex.getMessage().contains("No websocket fount at: /ws"));
  }

  // --- ROUTE EXECUTION TESTS ---

  @Test
  void testCallWithFullExecutionEnabled() throws Exception {
    Route mockRoute = mock(Route.class);
    when(mockRoute.getMethod()).thenReturn(Router.GET);

    // Setup Pipeline (Full execution) vs Handler (Default)
    Route.Handler pipeline = mock(Route.Handler.class);
    when(pipeline.apply(any())).thenReturn("PipelineResult");
    when(mockRoute.getPipeline()).thenReturn(pipeline);

    Handler handler = mock(Handler.class);
    when(handler.apply(any())).thenReturn("HandlerResult");
    when(mockRoute.getHandler()).thenReturn(handler);

    Router.Match match = mock(Router.Match.class);
    when(match.route()).thenReturn(mockRoute);
    when(match.pathMap()).thenReturn(new HashMap<>());
    when(app.match(any(Context.class))).thenReturn(match);

    // Default (Full Execution False)
    assertEquals("HandlerResult", router.get("/").value());

    // Full Execution True
    router.setFullExecution(true);
    assertEquals("PipelineResult", router.get("/").value());
  }

  @Test
  void testCallWithRspCallback() throws Exception {
    Route mockRoute = mock(Route.class);
    when(mockRoute.getMethod()).thenReturn(Router.GET);

    // Setup Pipeline (Full execution) vs Handler (Default)
    Route.Handler pipeline = mock(Route.Handler.class);
    when(pipeline.apply(any())).thenReturn("PipelineResult");
    when(mockRoute.getPipeline()).thenReturn(pipeline);

    Handler handler = mock(Handler.class);
    when(handler.apply(any())).thenReturn("HandlerResult");
    when(mockRoute.getHandler()).thenReturn(handler);

    Router.Match match = mock(Router.Match.class);
    when(match.route()).thenReturn(mockRoute);
    when(match.pathMap()).thenReturn(new HashMap<>());
    when(app.match(any(Context.class))).thenReturn(match);

    // Default (Full Execution False)
    router.call(
        "GET",
        "/",
        rsp -> {
          assertEquals("HandlerResult", rsp.value());
        });

    // Full Execution True
    router.setFullExecution(true);
    router.call(
        "GET",
        "/",
        rsp -> {
          assertEquals("PipelineResult", rsp.value());
        });
  }

  @Test
  void testCallWithCoroutine() throws Exception {
    Route mockRoute = mock(Route.class);
    when(mockRoute.getMethod()).thenReturn(Router.GET);
    when(mockRoute.getAttribute("coroutine")).thenReturn(Boolean.TRUE);

    Handler handler = mock(Handler.class);
    when(handler.apply(any())).thenReturn("CoroutineResult");
    when(mockRoute.getHandler()).thenReturn(handler);

    Router.Match match = mock(Router.Match.class);
    when(match.route()).thenReturn(mockRoute);
    when(match.pathMap()).thenReturn(new HashMap<>());
    when(app.match(any(Context.class))).thenReturn(match);

    MockResponse response = new MockResponse();
    response.setResult("CoroutineResult"); // Simulate background thread setting result

    // We must pass a custom MockContext to override the internal CountDownLatch logic,
    // otherwise the test will block indefinitely waiting for the latch.
    MockContext customCtx =
        new MockContext() {
          @Override
          public MockResponse getResponse() {
            MockResponse resp = super.getResponse();
            resp.getLatch().countDown(); // unblock immediately
            resp.setResult("CoroutineResult");
            return resp;
          }
        };

    assertEquals("CoroutineResult", router.get("/", customCtx, r -> {}).value());
    verify(app).setWorker(any(Executor.class)); // Verifies the singleThreadWorker fallback
  }

  @Test
  void testCallWithNonMockContext() throws Exception {
    Route mockRoute = mock(Route.class);
    when(mockRoute.getMethod()).thenReturn(Router.GET);

    Handler handler = mock(Handler.class);
    when(handler.apply(any())).thenReturn("RawContextResult");
    when(mockRoute.getHandler()).thenReturn(handler);

    Router.Match match = mock(Router.Match.class);
    when(match.route()).thenReturn(mockRoute);
    when(match.pathMap()).thenReturn(new HashMap<>());
    when(app.match(any(Context.class))).thenReturn(match);

    Context realContext = mock(Context.class);

    // If context is NOT a MockContext, it bypasses the Response mutation logic
    assertEquals("RawContextResult", router.get("/", realContext).value());
  }

  @Test
  void testCallExceptionPropagatesViaSneakyThrows() throws Exception {
    Route mockRoute = mock(Route.class);
    when(mockRoute.getMethod()).thenReturn(Router.GET);

    Handler handler = mock(Handler.class);
    when(handler.apply(any())).thenThrow(new IllegalArgumentException("Test Error"));
    when(mockRoute.getHandler()).thenReturn(handler);

    Router.Match match = mock(Router.Match.class);
    when(match.route()).thenReturn(mockRoute);
    when(match.pathMap()).thenReturn(new HashMap<>());
    when(app.match(any(Context.class))).thenReturn(match);

    assertThrows(IllegalArgumentException.class, () -> router.get("/"));
  }

  // --- CONTENT LENGTH CALCULATION TESTS ---

  @Test
  void testContentLengthCalculation() throws Exception {
    setupMockRoute(Router.GET, "abc"); // CharSequence
    router.get("/", res -> assertEquals(3, res.getContentLength()));

    setupMockRoute(Router.GET, 42); // Number
    router.get("/", res -> assertEquals(2, res.getContentLength()));

    setupMockRoute(Router.GET, true); // Boolean
    router.get("/", res -> assertEquals(4, res.getContentLength()));

    setupMockRoute(Router.GET, new byte[] {1, 2, 3, 4, 5}); // byte[]
    router.get("/", res -> assertEquals(5, res.getContentLength()));

    setupMockRoute(Router.GET, new Object()); // Unhandled type
    router.get("/", res -> assertEquals(-1, res.getContentLength()));
  }

  // --- ERROR HANDLER TESTS ---

  @Test
  void testTryErrorWithConsumer() {
    ErrorHandler handler = mock(ErrorHandler.class);
    when(app.getErrorHandler()).thenReturn(handler);
    when(app.errorCode(any(Throwable.class))).thenReturn(StatusCode.BAD_REQUEST);

    RuntimeException ex = new RuntimeException("Error");

    router.tryError(
        ex,
        response -> {
          // Just assert the consumer was called
          assertNotNull(response);
        });

    verify(handler).apply(any(Context.class), any(Throwable.class), any(StatusCode.class));
  }

  @Test
  void testTryErrorWithContext() {
    ErrorHandler handler = mock(ErrorHandler.class);
    when(app.getErrorHandler()).thenReturn(handler);
    when(app.errorCode(any(Throwable.class))).thenReturn(StatusCode.SERVER_ERROR);

    Context ctx = mock(Context.class);
    RuntimeException ex = new RuntimeException("Error");

    router.tryError(ex, ctx);

    verify(handler).apply(ctx, ex, StatusCode.SERVER_ERROR);
  }

  // --- HELPERS ---

  private void setupMockRoute(String method, Object result) throws Exception {
    Route mockRoute = mock(Route.class);
    when(mockRoute.getMethod()).thenReturn(method);

    Handler handler = mock(Handler.class);
    when(handler.apply(any())).thenReturn(result);
    when(mockRoute.getHandler()).thenReturn(handler);

    Router.Match match = mock(Router.Match.class);
    when(match.route()).thenReturn(mockRoute);
    when(match.pathMap()).thenReturn(new HashMap<>());

    when(app.match(any(Context.class))).thenReturn(match);
  }
}
