/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.grpc;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.util.concurrent.Flow.Subscription;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.grpc.ClientCall;
import io.grpc.MethodDescriptor;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import io.jooby.internal.grpc.GrpcRequestBridge;

public class GrpcRequestBridgeTest {

  private ClientCall<byte[], byte[]> call;
  private Subscription subscription;
  private ClientCallStreamObserver<byte[]> requestObserver;
  private ClientResponseObserver<byte[], byte[]> responseObserver;
  private GrpcRequestBridge bridge;

  @BeforeEach
  @SuppressWarnings("unchecked")
  public void setUp() {
    call = mock(ClientCall.class);
    subscription = mock(Subscription.class);
    requestObserver = mock(ClientCallStreamObserver.class);
    responseObserver = mock(ClientResponseObserver.class);

    // Default to BIDI_STREAMING to test standard flow-control and backpressure
    bridge = new GrpcRequestBridge(call, MethodDescriptor.MethodType.BIDI_STREAMING);
    bridge.setRequestObserver(requestObserver);
    bridge.setResponseObserver(responseObserver);
  }

  @Test
  @DisplayName("Should request initial demand (1) upon subscription")
  public void shouldRequestInitialDemandOnSubscribe() {
    bridge.onSubscribe(subscription);

    verify(subscription).request(1);
  }

  @Test
  @DisplayName("Should forward payload to requestObserver and request more if gRPC buffer is ready")
  public void shouldSendMessageAndRequestMoreIfReady() {
    bridge.onSubscribe(subscription);
    reset(subscription); // Clear the initial request(1) counter

    when(requestObserver.isReady()).thenReturn(true);

    byte[] payload = "test".getBytes();
    ByteBuffer frame = createFrame(payload);

    bridge.onNext(frame);

    ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
    verify(requestObserver).onNext(captor.capture());
    assertArrayEquals(payload, captor.getValue(), "The deframed payload should match exactly");

    // Because isReady() is true, it should demand the next network chunk
    verify(subscription).request(1);
  }

  @Test
  @DisplayName(
      "Should forward payload but apply backpressure (do not request) if gRPC is not ready")
  public void shouldNotRequestMoreIfNotReady() {
    bridge.onSubscribe(subscription);
    reset(subscription);

    when(requestObserver.isReady()).thenReturn(false);

    byte[] payload = "test".getBytes();
    bridge.onNext(createFrame(payload));

    verify(requestObserver).onNext(any());

    // Since isReady() is false, it should NOT request more data, effectively applying backpressure
    verify(subscription, never()).request(anyLong());
  }

  @Test
  @DisplayName("Should complete the requestObserver when the network stream completes")
  public void shouldCompleteRequestObserverOnComplete() {
    bridge.onSubscribe(subscription);
    bridge.onComplete();

    verify(requestObserver).onCompleted();
  }

  @Test
  @DisplayName("Should propagate network errors to the requestObserver")
  public void shouldPropagateErrorToObserver() {
    bridge.onSubscribe(subscription);
    Throwable error = new RuntimeException("Stream network failure");

    bridge.onError(error);

    verify(requestObserver).onError(error);
  }

  @Test
  @DisplayName("Unary calls should accumulate payload without forwarding until EOF")
  public void shouldHandleUnaryCallsDifferently() {
    bridge = new GrpcRequestBridge(call, MethodDescriptor.MethodType.UNARY);
    bridge.setResponseObserver(responseObserver);
    bridge.onSubscribe(subscription);
    reset(subscription);

    byte[] payload = "unary".getBytes();
    bridge.onNext(createFrame(payload));

    // For Unary and Server Streaming, chunks are NOT passed via onNext
    verify(requestObserver, never()).onNext(any());

    // It should keep requesting data from the network until EOF is reached
    verify(subscription).request(1);
  }

  @Test
  @DisplayName("onGrpcReady callback should trigger network demand if stream is active")
  public void shouldRequestMoreOnGrpcReady() {
    bridge.onSubscribe(subscription);
    reset(subscription);

    when(requestObserver.isReady()).thenReturn(true);

    bridge.onGrpcReady();

    verify(subscription).request(1);
  }

  private ByteBuffer createFrame(byte[] payload) {
    ByteBuffer frame = ByteBuffer.allocate(5 + payload.length);
    frame.put((byte) 0); // Uncompressed flag
    frame.putInt(payload.length);
    frame.put(payload);
    frame.flip(); // Prepare buffer for reading
    return frame;
  }
}
