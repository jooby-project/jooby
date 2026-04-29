/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.handler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.concurrent.Flow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.slf4j.Logger;

import io.jooby.*;
import io.jooby.output.Output;

public class ChunkedSubscriberTest {

  private Context ctx;
  private Route route;
  private MessageEncoder encoder;
  private Sender sender;
  private Flow.Subscription subscription;
  private MediaType mediaType;
  private Router router;
  private Logger logger;

  @BeforeEach
  void setUp() {
    // RETURNS_DEEP_STUBS automatically mocks ctx.getOutputFactory().newComposite()
    ctx = mock(Context.class, RETURNS_DEEP_STUBS);
    route = mock(Route.class);
    encoder = mock(MessageEncoder.class);
    sender = mock(Sender.class);
    subscription = mock(Flow.Subscription.class);
    mediaType = mock(MediaType.class);
    router = mock(Router.class);
    logger = mock(Logger.class);

    when(ctx.getRoute()).thenReturn(route);
    when(route.getEncoder()).thenReturn(encoder);
    when(ctx.responseSender()).thenReturn(sender);
    when(ctx.getResponseType()).thenReturn(mediaType);
    when(ctx.getRouter()).thenReturn(router);
    when(router.getLog()).thenReturn(logger);
  }

  @Test
  void testOnSubscribe() {
    ChunkedSubscriber sub = new ChunkedSubscriber(ctx);
    sub.onSubscribe(subscription);
    verify(subscription).request(1);
  }

  @Test
  void testOnNextNonJsonSuccess() throws Exception {
    Object item = new Object();
    Output data = mock(Output.class);
    when(encoder.encode(ctx, item)).thenReturn(data);
    when(mediaType.isJson()).thenReturn(false);

    ChunkedSubscriber sub = new ChunkedSubscriber(ctx);
    sub.onSubscribe(subscription);
    sub.onNext(item);

    ArgumentCaptor<Sender.Callback> captor = ArgumentCaptor.forClass(Sender.Callback.class);
    verify(sender).write(eq(data), captor.capture());

    // Simulate write success (x == null)
    captor.getValue().onComplete(ctx, null);

    // 1 request from onSubscribe, 1 request from successful callback
    verify(subscription, times(2)).request(1);
  }

  @Test
  void testOnNextWithAfterHandler() throws Exception {
    Object item = new Object();
    Output data = mock(Output.class);
    when(encoder.encode(ctx, item)).thenReturn(data);
    when(mediaType.isJson()).thenReturn(false);

    Route.After after = mock(Route.After.class);
    when(route.getAfter()).thenReturn(after);

    ChunkedSubscriber sub = new ChunkedSubscriber(ctx);
    sub.onSubscribe(subscription);
    sub.onNext(item);

    verify(after).apply(ctx, item, null);
    verify(sender).write(eq(data), any(Sender.Callback.class));
  }

  @Test
  void testOnNextJsonFirstAndSecond() throws Exception {
    Object item1 = new Object();
    Object item2 = new Object();
    Output data = mock(Output.class);
    when(encoder.encode(eq(ctx), any())).thenReturn(data);
    when(mediaType.isJson()).thenReturn(true);

    ChunkedSubscriber sub = new ChunkedSubscriber(ctx);
    sub.onSubscribe(subscription);

    // First item -> Covers responseType == null && responseType.isJson() (Prepends '[')
    sub.onNext(item1);
    ArgumentCaptor<Sender.Callback> captor = ArgumentCaptor.forClass(Sender.Callback.class);
    verify(sender, times(1)).write(any(Output.class), captor.capture());

    captor.getValue().onComplete(ctx, null);

    // Second item -> Covers responseType != null && responseType.isJson() (Prepends ',')
    sub.onNext(item2);
    verify(sender, times(2)).write(any(Output.class), captor.capture());
  }

  @Test
  void testOnNextNonJsonSecondItem() throws Exception {
    Object item1 = new Object();
    Object item2 = new Object();
    Output data = mock(Output.class);
    when(encoder.encode(eq(ctx), any())).thenReturn(data);
    when(mediaType.isJson()).thenReturn(false);

    ChunkedSubscriber sub = new ChunkedSubscriber(ctx);
    sub.onSubscribe(subscription);

    sub.onNext(item1);

    // Second item -> Covers responseType != null && !responseType.isJson()
    sub.onNext(item2);

    verify(sender, times(2)).write(eq(data), any(Sender.Callback.class));
  }

  @Test
  void testOnNextExceptionInEncode() throws Exception {
    Object item = new Object();
    RuntimeException ex = new RuntimeException("encoder error");
    when(encoder.encode(ctx, item)).thenThrow(ex);

    when(ctx.getMethod()).thenReturn("GET");
    when(ctx.getRequestPath()).thenReturn("/path");

    ChunkedSubscriber sub = new ChunkedSubscriber(ctx);
    sub.onSubscribe(subscription);
    sub.onNext(item);

    verify(subscription).cancel();
    verify(ctx).sendError(ex);
  }

  @Test
  void testOnNextCallbackError() throws Exception {
    Object item = new Object();
    Output data = mock(Output.class);
    when(encoder.encode(ctx, item)).thenReturn(data);
    when(mediaType.isJson()).thenReturn(false);

    when(ctx.getMethod()).thenReturn("GET");
    when(ctx.getRequestPath()).thenReturn("/path");

    ChunkedSubscriber sub = new ChunkedSubscriber(ctx);
    sub.onSubscribe(subscription);
    sub.onNext(item);

    ArgumentCaptor<Sender.Callback> captor = ArgumentCaptor.forClass(Sender.Callback.class);
    verify(sender).write(eq(data), captor.capture());

    Exception ex = new Exception("write error");

    captor.getValue().onComplete(ctx, ex);

    verify(subscription).cancel();
    verify(ctx).sendError(ex);
  }

  @Test
  void testOnErrorDirectly() {
    ChunkedSubscriber sub = new ChunkedSubscriber(ctx);
    sub.onSubscribe(subscription);
    Exception ex = new Exception("main error");

    sub.onError(ex);

    verify(ctx).sendError(ex);
    verify(subscription, never()).cancel(); // direct onError call passes false to cancel parameter
  }

  @Test
  void testOnErrorConnectionLost() {
    ChunkedSubscriber sub = new ChunkedSubscriber(ctx);
    sub.onSubscribe(subscription);
    Exception ex = new Exception("main error");
    when(ctx.getMethod()).thenReturn("GET");
    when(ctx.getRequestPath()).thenReturn("/path");

    try (MockedStatic<Server> serverMock = mockStatic(Server.class)) {
      serverMock.when(() -> Server.connectionLost(ex)).thenReturn(true);
      sub.onError(ex);

      verify(logger).debug("connection lost: {} {}", "GET", "/path", ex);
      verify(ctx, never()).sendError(any());
    }
  }

  @Test
  void testOnErrorAfterHandlerThrows() throws Exception {
    ChunkedSubscriber sub = new ChunkedSubscriber(ctx);
    sub.onSubscribe(subscription);

    Route.After after = mock(Route.After.class);
    when(route.getAfter()).thenReturn(after);

    RuntimeException unexpected = new RuntimeException("unexpected error in after");
    doThrow(unexpected).when(after).apply(eq(ctx), isNull(), any(Throwable.class));

    Exception ex = new Exception("main error");
    sub.onError(ex);

    verify(ctx).sendError(ex);
    assertTrue(Arrays.asList(ex.getSuppressed()).contains(unexpected));
  }

  @Test
  void testOnCompleteNonJson() {
    ChunkedSubscriber sub = new ChunkedSubscriber(ctx);

    sub.onComplete();

    verify(sender).close();
    verify(sender, never()).write(any(byte[].class), any(Sender.Callback.class));
  }

  @Test
  void testOnCompleteJsonSuccess() throws Exception {
    Object item = new Object();
    Output data = mock(Output.class);
    when(encoder.encode(ctx, item)).thenReturn(data);
    when(mediaType.isJson()).thenReturn(true);

    ChunkedSubscriber sub = new ChunkedSubscriber(ctx);
    sub.onSubscribe(subscription);
    sub.onNext(item); // Initialize responseType to JSON
    reset(sender); // Clear the interaction counts from onNext

    sub.onComplete();

    ArgumentCaptor<Sender.Callback> captor = ArgumentCaptor.forClass(Sender.Callback.class);
    // Assert that the ']' byte array was written out
    verify(sender).write(any(byte[].class), captor.capture());

    captor.getValue().onComplete(ctx, null);

    verify(sender).close();
    verify(ctx, never()).sendError(any());
  }

  @Test
  void testOnCompleteJsonError() throws Exception {
    Object item = new Object();
    Output data = mock(Output.class);
    when(encoder.encode(ctx, item)).thenReturn(data);
    when(mediaType.isJson()).thenReturn(true);

    when(ctx.getMethod()).thenReturn("GET");
    when(ctx.getRequestPath()).thenReturn("/path");

    ChunkedSubscriber sub = new ChunkedSubscriber(ctx);
    sub.onSubscribe(subscription);
    sub.onNext(item);
    reset(sender);

    sub.onComplete();

    ArgumentCaptor<Sender.Callback> captor = ArgumentCaptor.forClass(Sender.Callback.class);
    verify(sender).write(any(byte[].class), captor.capture());

    Exception err = new Exception("complete callback error");

    captor.getValue().onComplete(ctx, err);

    verify(ctx).sendError(err);
  }
}
