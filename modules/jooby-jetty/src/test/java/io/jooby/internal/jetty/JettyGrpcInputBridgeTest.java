/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Flow;

import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.Callback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JettyGrpcInputBridgeTest {

  @Mock Request request;
  @Mock Flow.Subscriber<ByteBuffer> subscriber;
  @Mock Callback callback;

  private JettyGrpcInputBridge bridge;

  @BeforeEach
  void setup() {
    bridge = new JettyGrpcInputBridge(request, subscriber, callback);
  }

  @Test
  void testStart_SubscribesToFlow() {
    bridge.start();
    verify(subscriber).onSubscribe(bridge);
  }

  @Test
  void testRequest_NegativeDemand_TriggersError() {
    bridge.request(0);
    bridge.request(-5);

    ArgumentCaptor<IllegalArgumentException> captor =
        ArgumentCaptor.forClass(IllegalArgumentException.class);
    verify(subscriber, times(2)).onError(captor.capture());
    assertEquals("Demand must be positive", captor.getValue().getMessage());
    verify(request, never()).read(); // Ensure run() was bypassed
  }

  @Test
  void testRequest_IgnoresWhenDemandAlreadyPositive() {
    when(request.read()).thenReturn(null); // Keeps demand at 1 when run() breaks

    // Demand: 0 -> 1. Triggers run(), gets null, calls request.demand(bridge).
    bridge.request(1);
    verify(request, times(1)).demand(bridge);

    // Demand: 1 -> 2. Because it's already > 0, run() is NOT triggered again concurrently.
    bridge.request(1);

    // Verify demand() wasn't called a second time
    verify(request, times(1)).demand(bridge);
  }

  @Test
  void testCancel_ResetsDemandAndFailsCallback() {
    bridge.cancel();

    ArgumentCaptor<CancellationException> captor =
        ArgumentCaptor.forClass(CancellationException.class);
    verify(callback).failed(captor.capture());
    assertEquals("gRPC stream cancelled by client", captor.getValue().getMessage());
  }

  @Test
  void testRun_ChunkIsNull_DemandsMore() {
    when(request.read()).thenReturn(null);

    bridge.request(1);

    verify(request).demand(bridge);
    verify(subscriber, never()).onNext(any());
  }

  @Test
  void testRun_ChunkHasFailure_TerminatesStream() {
    Content.Chunk chunk = mock(Content.Chunk.class);
    Throwable failure = new RuntimeException("Chunk Failure");
    when(chunk.getFailure()).thenReturn(failure);

    when(request.read()).thenReturn(chunk);

    bridge.request(1);

    verify(subscriber).onError(failure);
    verify(callback).failed(failure);
    verify(chunk).release(); // Verifies finally block executed
  }

  @Test
  void testRun_ChunkHasBuffer_NotLast() {
    Content.Chunk chunk = mock(Content.Chunk.class);
    ByteBuffer buffer = ByteBuffer.allocate(10);
    // Artificially ensure hasRemaining() is true
    buffer.position(0);

    when(chunk.getFailure()).thenReturn(null);
    when(chunk.getByteBuffer()).thenReturn(buffer);
    when(chunk.isLast()).thenReturn(false);

    when(request.read()).thenReturn(chunk);

    bridge.request(1); // loop runs once because demand is decremented to 0

    verify(subscriber).onNext(buffer);
    verify(chunk).release();
    verify(subscriber, never()).onComplete();
  }

  @Test
  void testRun_ChunkHasBuffer_IsLast() {
    Content.Chunk chunk = mock(Content.Chunk.class);
    ByteBuffer buffer = ByteBuffer.allocate(10);

    when(chunk.getFailure()).thenReturn(null);
    when(chunk.getByteBuffer()).thenReturn(buffer);
    when(chunk.isLast()).thenReturn(true);

    when(request.read()).thenReturn(chunk);

    bridge.request(1);

    verify(subscriber).onNext(buffer);
    verify(chunk).release();
    verify(subscriber).onComplete();
  }

  @Test
  void testRun_ChunkHasEmptyBuffer_LoopsAndDemands() {
    Content.Chunk chunk = mock(Content.Chunk.class);
    ByteBuffer buffer = ByteBuffer.allocate(0); // hasRemaining() == false

    when(chunk.getFailure()).thenReturn(null);
    when(chunk.getByteBuffer()).thenReturn(buffer);
    when(chunk.isLast()).thenReturn(false);

    // First read returns empty chunk (demand stays 1), second returns null to break loop safely
    when(request.read()).thenReturn(chunk).thenReturn(null);

    bridge.request(1);

    verify(subscriber, never()).onNext(any());
    verify(chunk).release();
    verify(request).demand(bridge);
  }

  @Test
  void testRun_ChunkBufferIsNull_LoopsAndDemands() {
    Content.Chunk chunk = mock(Content.Chunk.class);

    when(chunk.getFailure()).thenReturn(null);
    when(chunk.getByteBuffer()).thenReturn(null);
    when(chunk.isLast()).thenReturn(false);

    // First read returns chunk with null buffer (demand stays 1), second returns null to break loop
    // safely
    when(request.read()).thenReturn(chunk).thenReturn(null);

    bridge.request(1);

    verify(subscriber, never()).onNext(any());
    verify(chunk).release();
    verify(request).demand(bridge);
  }

  @Test
  void testRun_CatchesGlobalThrowable() {
    RuntimeException ex = new RuntimeException("Unexpected core read error");
    when(request.read()).thenThrow(ex);

    bridge.request(1);

    verify(subscriber).onError(ex);
    verify(callback).failed(ex);
  }
}
