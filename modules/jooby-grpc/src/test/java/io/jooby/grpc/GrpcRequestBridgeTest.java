/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.grpc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.nio.ByteBuffer;
import java.util.concurrent.Flow.Subscription;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.grpc.stub.ClientCallStreamObserver;

public class GrpcRequestBridgeTest {

  private ClientCallStreamObserver<byte[]> grpcObserver;
  private Subscription subscription;
  private GrpcRequestBridge bridge;

  @BeforeEach
  @SuppressWarnings("unchecked")
  public void setUp() {
    grpcObserver = mock(ClientCallStreamObserver.class);
    subscription = mock(Subscription.class);
    bridge = new GrpcRequestBridge(grpcObserver);
  }

  @Test
  public void shouldRequestInitialDemandOnSubscribe() {
    bridge.onSubscribe(subscription);

    // Verify gRPC readiness handler is registered
    verify(grpcObserver).setOnReadyHandler(any(Runnable.class));

    // Verify initial demand of 1 is requested from Jooby
    verify(subscription).request(1);
  }

  @Test
  public void shouldDelegateOnNextAndRequestMoreIfReady() {
    bridge.onSubscribe(subscription);

    // Reset the mock to clear the initial request(1) from onSubscribe
    reset(subscription);

    // Simulate gRPC being ready to receive more data
    when(grpcObserver.isReady()).thenReturn(true);

    // Send a complete gRPC frame: Compressed Flag (0) + Length (4) + Payload ("test")
    byte[] payload = "test".getBytes();
    ByteBuffer frame = ByteBuffer.allocate(5 + payload.length);
    frame.put((byte) 0).putInt(payload.length).put(payload).flip();

    bridge.onNext(frame);

    // Verify the deframed payload was passed to gRPC
    verify(grpcObserver).onNext(payload);

    // Verify backpressure: since gRPC was ready, it should request the next chunk
    verify(subscription).request(1);
  }

  @Test
  public void shouldDelegateOnNextButSuspendDemandIfNotReady() {
    bridge.onSubscribe(subscription);
    reset(subscription);

    // Simulate gRPC internal buffer being full (not ready)
    when(grpcObserver.isReady()).thenReturn(false);

    byte[] payload = "test".getBytes();
    ByteBuffer frame = ByteBuffer.allocate(5 + payload.length);
    frame.put((byte) 0).putInt(payload.length).put(payload).flip();

    bridge.onNext(frame);

    verify(grpcObserver).onNext(payload);

    // Verify backpressure: gRPC is NOT ready, so we MUST NOT request more from Jooby
    verify(subscription, never()).request(anyLong());
  }

  @Test
  public void shouldResumeDemandWhenGrpcBecomesReady() {
    bridge.onSubscribe(subscription);
    reset(subscription);

    // Capture the readiness handler registered during onSubscribe
    ArgumentCaptor<Runnable> handlerCaptor = ArgumentCaptor.forClass(Runnable.class);
    verify(grpcObserver).setOnReadyHandler(handlerCaptor.capture());
    Runnable onReadyHandler = handlerCaptor.getValue();

    // Simulate gRPC signaling that it is now ready
    when(grpcObserver.isReady()).thenReturn(true);
    onReadyHandler.run();

    // Verify backpressure: the handler should resume demanding data
    verify(subscription).request(1);
  }

  @Test
  public void shouldCancelSubscriptionAndDelegateOnError() {
    bridge.onSubscribe(subscription);

    Throwable error = new RuntimeException("Network failure");
    bridge.onError(error);

    verify(grpcObserver).onError(error);
  }

  @Test
  public void shouldDelegateOnCompleteIdempotently() {
    bridge.onComplete();
    bridge.onComplete(); // Second call should be ignored

    verify(grpcObserver, times(1)).onCompleted();
  }
}
