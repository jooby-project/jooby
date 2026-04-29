/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Flow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import io.jooby.Context;
import io.jooby.ReactiveSupport;
import io.jooby.Route;

class ConcurrentHandlerTest {

  private Context ctx;
  private Route route;
  private Route.Handler next;
  private Route.After after;

  @BeforeEach
  void setUp() {
    ctx = mock(Context.class);
    route = mock(Route.class);
    next = mock(Route.Handler.class);
    after = mock(Route.After.class);

    when(ctx.getRoute()).thenReturn(route);
  }

  @Test
  @DisplayName("Verify setRoute makes the route non-blocking")
  void testSetRoute() {
    ConcurrentHandler handler = new ConcurrentHandler();
    handler.setRoute(route);
    verify(route).setNonBlocking(true);
  }

  @Test
  @DisplayName("Verify toString returns 'concurrent'")
  void testToString() {
    ConcurrentHandler handler = new ConcurrentHandler();
    assertEquals("concurrent", handler.toString());
  }

  @Test
  @DisplayName("Verify execution short-circuits if response is already started")
  void testResponseAlreadyStarted() throws Exception {
    when(next.apply(ctx)).thenReturn("Some Result");
    when(ctx.isResponseStarted()).thenReturn(true);

    ConcurrentHandler handler = new ConcurrentHandler();
    Object result = handler.apply(next).apply(ctx);

    // Returns context to mark as handled
    assertEquals(ctx, result);
    verify(ctx, never()).render(any());
  }

  @Test
  @DisplayName("Verify Flow.Publisher result triggers subscription")
  void testFlowPublisher() throws Exception {
    Flow.Publisher<?> publisher = mock(Flow.Publisher.class);
    when(next.apply(ctx)).thenReturn(publisher);
    when(ctx.isResponseStarted()).thenReturn(false);

    Flow.Subscriber subscriber = mock(Flow.Subscriber.class);

    try (MockedStatic<ReactiveSupport> rs = mockStatic(ReactiveSupport.class)) {
      rs.when(() -> ReactiveSupport.newSubscriber(ctx)).thenReturn(subscriber);

      ConcurrentHandler handler = new ConcurrentHandler();
      Object result = handler.apply(next).apply(ctx);

      // Verify the publisher gets subscribed to, and context is returned
      verify(publisher).subscribe(subscriber);
      assertEquals(ctx, result);
    }
  }

  @Test
  @DisplayName("Verify standard non-async result is returned as-is")
  void testStandardResult() throws Exception {
    Object expectedResult = "Plain Old Java Object";
    when(next.apply(ctx)).thenReturn(expectedResult);
    when(ctx.isResponseStarted()).thenReturn(false);

    ConcurrentHandler handler = new ConcurrentHandler();
    Object result = handler.apply(next).apply(ctx);

    assertEquals(expectedResult, result);
  }

  @Test
  @DisplayName("CompletionStage: Verify successful value rendering")
  void testCompletionStageSuccess() throws Exception {
    CompletableFuture<String> future = new CompletableFuture<>();
    when(next.apply(ctx)).thenReturn(future);
    when(ctx.isResponseStarted()).thenReturn(false); // First check
    when(route.getAfter()).thenReturn(null);

    ConcurrentHandler handler = new ConcurrentHandler();
    Object result = handler.apply(next).apply(ctx);
    assertEquals(ctx, result);

    // Complete the future to trigger the callback
    future.complete("Hello Async");

    // Because it wasn't started and value isn't ctx, it should render
    verify(ctx).render("Hello Async");
  }

  @Test
  @DisplayName("CompletionStage: Verify After handler is executed")
  void testCompletionStageWithAfterHandler() throws Exception {
    CompletableFuture<String> future = new CompletableFuture<>();
    when(next.apply(ctx)).thenReturn(future);
    when(route.getAfter()).thenReturn(after);

    ConcurrentHandler handler = new ConcurrentHandler();
    handler.apply(next).apply(ctx);

    future.complete("Hello After");

    verify(after).apply(ctx, "Hello After", null);
    verify(ctx).render("Hello After");
  }

  @Test
  @DisplayName("CompletionStage: Skip render if response was started asynchronously")
  void testCompletionStageSkipRenderIfStarted() throws Exception {
    CompletableFuture<String> future = new CompletableFuture<>();
    when(next.apply(ctx)).thenReturn(future);

    // Simulate that by the time the future completes, the response has been started
    when(ctx.isResponseStarted()).thenReturn(false, true);

    ConcurrentHandler handler = new ConcurrentHandler();
    handler.apply(next).apply(ctx);

    future.complete("Value");

    verify(ctx, never()).render(any());
  }

  @Test
  @DisplayName("CompletionStage: Skip render if value is the Context itself or null")
  void testCompletionStageSkipRenderIfContextOrNull() throws Exception {
    CompletableFuture<Object> future = new CompletableFuture<>();
    when(next.apply(ctx)).thenReturn(future);

    ConcurrentHandler handler = new ConcurrentHandler();
    handler.apply(next).apply(ctx);

    // Complete with context -> should not render
    future.complete(ctx);
    verify(ctx, never()).render(any());

    // Complete with null -> should not render
    CompletableFuture<Object> nullFuture = new CompletableFuture<>();
    when(next.apply(ctx)).thenReturn(nullFuture);
    handler.apply(next).apply(ctx);
    nullFuture.complete(null);

    verify(ctx, never()).render(any());
  }

  @Test
  @DisplayName("CompletionStage: Verify standard Exception is sent to Error handler")
  void testCompletionStageStandardException() throws Exception {
    CompletableFuture<String> future = new CompletableFuture<>();
    when(next.apply(ctx)).thenReturn(future);

    ConcurrentHandler handler = new ConcurrentHandler();
    handler.apply(next).apply(ctx);

    RuntimeException ex = new RuntimeException("Async Boom");
    future.completeExceptionally(ex);

    verify(ctx).sendError(ex);
  }

  @Test
  @DisplayName("CompletionStage: Verify CompletionException is correctly unwrapped")
  void testCompletionStageUnwrapException() throws Exception {
    CompletableFuture<String> future = new CompletableFuture<>();
    when(next.apply(ctx)).thenReturn(future);

    ConcurrentHandler handler = new ConcurrentHandler();
    handler.apply(next).apply(ctx);

    IllegalArgumentException cause = new IllegalArgumentException("Root cause");
    CompletionException wrap = new CompletionException(cause);
    future.completeExceptionally(wrap);

    // Assert that the unwrapped cause is sent to the error handler
    verify(ctx).sendError(cause);
  }

  @Test
  @DisplayName("CompletionStage: Verify CompletionException with NO cause returns itself")
  void testCompletionStageUnwrapExceptionNoCause() throws Exception {
    CompletableFuture<String> future = new CompletableFuture<>();
    when(next.apply(ctx)).thenReturn(future);

    ConcurrentHandler handler = new ConcurrentHandler();
    handler.apply(next).apply(ctx);

    CompletionException wrap = new CompletionException(null);
    future.completeExceptionally(wrap);

    // If there's no cause, the wrapper itself is sent
    verify(ctx).sendError(wrap);
  }

  @Test
  @DisplayName("CompletionStage: Verify exceptions thrown INSIDE the callback are caught")
  void testCompletionStageCallbackThrowsException() throws Exception {
    CompletableFuture<String> future = new CompletableFuture<>();
    when(next.apply(ctx)).thenReturn(future);

    RuntimeException afterError = new RuntimeException("Error inside After block");
    when(route.getAfter()).thenReturn(after);
    doThrow(afterError).when(after).apply(any(), any(), any());

    ConcurrentHandler handler = new ConcurrentHandler();
    handler.apply(next).apply(ctx);

    future.complete("Trigger Error");

    // The catch block around the entire callback should catch this and send to sendError
    verify(ctx).sendError(afterError);
  }
}
