/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.vertx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.OutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import io.jooby.Context;
import io.jooby.Route;
import io.jooby.Router;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;

@ExtendWith(MockitoExtension.class)
class VertxHandlerTest {

  @Mock private Context ctx;
  @Mock private Route.Handler next;
  @Mock private Router router;
  @Mock private Logger logger;

  private VertxHandler handler;

  @BeforeEach
  void setUp() {
    handler = new VertxHandler();
  }

  @Test
  void shouldExposeStaticFilter() {
    assertNotNull(VertxHandler.vertx());
  }

  @Test
  void shouldReturnContextIfResponseAlreadyStarted() throws Exception {
    when(next.apply(ctx)).thenReturn(new Object());
    when(ctx.isResponseStarted()).thenReturn(true);

    Object result = handler.apply(next).apply(ctx);

    assertEquals(ctx, result);
  }

  @Test
  void shouldReturnResultIfUnhandledType() throws Exception {
    Object expectedResult = "Standard String Result";
    when(next.apply(ctx)).thenReturn(expectedResult);
    when(ctx.isResponseStarted()).thenReturn(false);

    Object actualResult = handler.apply(next).apply(ctx);

    assertEquals(expectedResult, actualResult);
  }

  @Test
  void shouldHandleSynchronousBuffer() throws Exception {
    Buffer buffer = mock(Buffer.class);
    byte[] bytes = new byte[] {1, 2, 3};
    when(buffer.getBytes()).thenReturn(bytes);
    when(ctx.send(bytes)).thenReturn(ctx);

    when(next.apply(ctx)).thenReturn(buffer);
    when(ctx.isResponseStarted()).thenReturn(false);

    Object result = handler.apply(next).apply(ctx);

    assertEquals(ctx, result);
    verify(ctx).send(bytes);
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldHandlePromise() throws Exception {
    Promise<String> promise = mock(Promise.class);
    Future<String> future = mock(Future.class);
    when(promise.future()).thenReturn(future);

    when(next.apply(ctx)).thenReturn(promise);
    when(ctx.isResponseStarted()).thenReturn(false);

    Object result = handler.apply(next).apply(ctx);

    assertEquals(ctx, result);
    verify(future).onComplete(any(Handler.class));
  }

  @Test
  void shouldHandleFutureWithBuffer() throws Exception {
    // 1. Create and configure the mock first
    Buffer buffer = mock(Buffer.class);
    byte[] bytes = new byte[] {4, 5, 6};
    when(buffer.getBytes()).thenReturn(bytes);

    // 2. Pass the configured mock into the future simulation
    Future<Buffer> future = simulateFutureCompletion(buffer, null);

    when(next.apply(ctx)).thenReturn(future);
    when(ctx.isResponseStarted()).thenReturn(false);

    handler.apply(next).apply(ctx);

    verify(ctx).send(bytes);
  }

  @Test
  void shouldHandleFutureWithArbitraryObject() throws Exception {
    Object value = new Object();
    Future<Object> future = simulateFutureCompletion(value, null);

    when(next.apply(ctx)).thenReturn(future);
    when(ctx.isResponseStarted()).thenReturn(false);

    handler.apply(next).apply(ctx);

    verify(ctx).render(value);
  }

  @Test
  void shouldHandleFutureFailure() throws Exception {
    Throwable error = new RuntimeException("Future failed");
    Future<Object> future = simulateFutureCompletion(null, error);

    when(next.apply(ctx)).thenReturn(future);
    when(ctx.isResponseStarted()).thenReturn(false);

    handler.apply(next).apply(ctx);

    verify(ctx).sendError(error);
  }

  // --- AsyncFileHandler branch tests ---

  @Test
  void asyncFileHandler_shouldWriteBufferAndCloseSuccessfully() throws Exception {
    OutputStream out = mock(OutputStream.class);
    when(ctx.responseStream()).thenReturn(out);

    AsyncFile file = mock(AsyncFile.class);
    Future<AsyncFile> future = simulateFutureCompletion(file, null);
    when(next.apply(ctx)).thenReturn(future);

    handler.apply(next).apply(ctx);

    // Capture the attached handlers
    ArgumentCaptor<Handler<Buffer>> handleCaptor = ArgumentCaptor.forClass(Handler.class);
    ArgumentCaptor<Handler<Void>> endCaptor = ArgumentCaptor.forClass(Handler.class);

    verify(file).handler(handleCaptor.capture());
    verify(file).endHandler(endCaptor.capture());

    // 1. Test Writing
    Buffer buffer = mock(Buffer.class);
    byte[] bytes = new byte[] {7, 8, 9};
    when(buffer.getBytes()).thenReturn(bytes);

    handleCaptor.getValue().handle(buffer);
    verify(out).write(bytes);

    // 2. Test End
    endCaptor.getValue().handle(null);
    verify(out).close();
    verify(file).close();
  }

  @Test
  void asyncFileHandler_shouldHandleIOExceptionOnWrite() throws Exception {
    OutputStream out = mock(OutputStream.class);
    when(ctx.responseStream()).thenReturn(out);

    AsyncFile file = mock(AsyncFile.class);
    Future<AsyncFile> future = simulateFutureCompletion(file, null);
    when(next.apply(ctx)).thenReturn(future);

    handler.apply(next).apply(ctx);

    ArgumentCaptor<Handler<Buffer>> handleCaptor = ArgumentCaptor.forClass(Handler.class);
    verify(file).handler(handleCaptor.capture());

    Buffer buffer = mock(Buffer.class);
    when(buffer.getBytes()).thenReturn(new byte[] {0});

    IOException writeError = new IOException("Disk full");
    doThrow(writeError).when(out).write(any(byte[].class));

    handleCaptor.getValue().handle(buffer);

    // Should close file and send error
    verify(file).close();
    verify(ctx).sendError(writeError);
  }

  @Test
  void asyncFileHandler_shouldHandleIOExceptionOnClose() throws Exception {
    OutputStream out = mock(OutputStream.class);
    when(ctx.responseStream()).thenReturn(out);

    AsyncFile file = mock(AsyncFile.class);
    Future<AsyncFile> future = simulateFutureCompletion(file, null);
    when(next.apply(ctx)).thenReturn(future);

    handler.apply(next).apply(ctx);

    ArgumentCaptor<Handler<Void>> endCaptor = ArgumentCaptor.forClass(Handler.class);
    verify(file).endHandler(endCaptor.capture());

    IOException closeError = new IOException("Stream closed prematurely");
    doThrow(closeError).when(out).close();

    endCaptor.getValue().handle(null);

    // Even if out.close() throws, file.close() should be called via finally block
    // AND it should call handleError which also closes, but the finally block ensures it.
    verify(ctx).sendError(closeError);
    verify(file, atLeastOnce()).close();
  }

  @Test
  void asyncFileHandler_shouldRouteMultipleErrorsToLogger() throws Exception {
    OutputStream out = mock(OutputStream.class);
    when(ctx.responseStream()).thenReturn(out);
    when(ctx.getRouter()).thenReturn(router);
    when(router.getLog()).thenReturn(logger);

    AsyncFile file = mock(AsyncFile.class);
    Future<AsyncFile> future = simulateFutureCompletion(file, null);
    when(next.apply(ctx)).thenReturn(future);

    handler.apply(next).apply(ctx);

    ArgumentCaptor<Handler<Throwable>> errCaptor = ArgumentCaptor.forClass(Handler.class);
    verify(file).exceptionHandler(errCaptor.capture());

    Throwable firstError = new RuntimeException("First Error");
    Throwable secondError = new RuntimeException("Second Error");

    errCaptor.getValue().handle(firstError);
    errCaptor.getValue().handle(secondError);

    // First error triggers standard error response
    verify(ctx).sendError(firstError);

    // Second error is intercepted by the logger because it's already errored
    verify(logger).error(eq("Async file write resulted in exception"), eq(secondError));

    // Context sendError should strictly only be called once
    verify(ctx, times(1)).sendError(any(Throwable.class));
  }

  @Test
  void asyncFileHandler_shouldIgnoreFileCloseExceptionsInErrorHandler() throws Exception {
    OutputStream out = mock(OutputStream.class);
    when(ctx.responseStream()).thenReturn(out);

    AsyncFile file = mock(AsyncFile.class);
    doThrow(new IllegalStateException("File already closed")).when(file).close();

    Future<AsyncFile> future = simulateFutureCompletion(file, null);
    when(next.apply(ctx)).thenReturn(future);

    handler.apply(next).apply(ctx);

    ArgumentCaptor<Handler<Throwable>> errCaptor = ArgumentCaptor.forClass(Handler.class);
    verify(file).exceptionHandler(errCaptor.capture());

    Throwable originalError = new RuntimeException("Original error");

    // This will trigger handleError, which will try file.close() and swallow the
    // IllegalStateException
    errCaptor.getValue().handle(originalError);

    verify(ctx).sendError(originalError);
  }

  @Test
  void asyncFileHandler_shouldDoNothingIfAlreadyClosed() throws Exception {
    OutputStream out = mock(OutputStream.class);
    when(ctx.responseStream()).thenReturn(out);

    AsyncFile file = mock(AsyncFile.class);
    Future<AsyncFile> future = simulateFutureCompletion(file, null);
    when(next.apply(ctx)).thenReturn(future);

    handler.apply(next).apply(ctx);

    ArgumentCaptor<Handler<Buffer>> handleCaptor = ArgumentCaptor.forClass(Handler.class);
    ArgumentCaptor<Handler<Void>> endCaptor = ArgumentCaptor.forClass(Handler.class);
    verify(file).handler(handleCaptor.capture());
    verify(file).endHandler(endCaptor.capture());

    // Trigger end (marks as closed)
    endCaptor.getValue().handle(null);
    verify(out, times(1)).close();

    // Trigger end again (should be no-op due to !closed check)
    endCaptor.getValue().handle(null);
    verify(out, times(1)).close(); // Still 1

    // Trigger handle write on closed stream (should be no-op due to !closed check)
    Buffer buffer = mock(Buffer.class);
    handleCaptor.getValue().handle(buffer);
    verify(out, never()).write(any(byte[].class));
  }

  @SuppressWarnings("unchecked")
  private <T> Future<T> simulateFutureCompletion(T result, Throwable error) {
    Future<T> future = mock(Future.class);

    // We removed the future.result() stubbing here because the
    // handler only calls ar.result() inside the onComplete block.

    doAnswer(
            invocation -> {
              Handler<AsyncResult<T>> arHandler = invocation.getArgument(0);
              AsyncResult<T> ar = mock(AsyncResult.class);
              if (error == null) {
                when(ar.succeeded()).thenReturn(true);
                when(ar.result()).thenReturn(result);
              } else {
                when(ar.succeeded()).thenReturn(false);
                when(ar.cause()).thenReturn(error);
              }
              arHandler.handle(ar);
              return future;
            })
        .when(future)
        .onComplete(any(Handler.class));

    return future;
  }
}
