/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.grpc;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.util.concurrent.Flow.Subscription;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import io.grpc.ClientCall;
import io.grpc.MethodDescriptor.MethodType;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.ClientResponseObserver;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class GrpcRequestBridgeTest {

  @Mock ClientCall<byte[], byte[]> call;
  @Mock ClientResponseObserver<byte[], byte[]> responseObserver;
  @Mock ClientCallStreamObserver<byte[]> requestObserver;
  @Mock Subscription subscription;

  private GrpcRequestBridge unaryBridge;
  private GrpcRequestBridge streamingBridge;

  @BeforeEach
  void setup() {
    unaryBridge = new GrpcRequestBridge(call, MethodType.UNARY);
    streamingBridge = new GrpcRequestBridge(call, MethodType.BIDI_STREAMING);
  }

  // --- HELPER ---

  /** Generates a properly framed gRPC ByteBuffer payload to bypass the internal GrpcDeframer */
  private ByteBuffer createGrpcFrame(byte[] payload) {
    ByteBuffer buffer = ByteBuffer.allocate(5 + payload.length);
    buffer.put((byte) 0); // Uncompressed
    buffer.putInt(payload.length);
    buffer.put(payload);
    buffer.flip();
    return buffer;
  }

  // --- ONSUBSCRIBE & ONGRPCREADY TESTS ---

  @Test
  void testOnSubscribe_RequestsOne() {
    unaryBridge.onSubscribe(subscription);
    verify(subscription).request(1);
  }

  @Test
  void testOnGrpcReady_AllConditionsMet_RequestsOne() {
    streamingBridge.onSubscribe(subscription); // sets subscription
    streamingBridge.setRequestObserver(requestObserver);
    when(requestObserver.isReady()).thenReturn(true);

    streamingBridge.onGrpcReady();

    // 1 from onSubscribe, 1 from onGrpcReady
    verify(subscription, times(2)).request(1);
  }

  @Test
  void testOnGrpcReady_ObserverNotReady_DoesNothing() {
    streamingBridge.onSubscribe(subscription);
    streamingBridge.setRequestObserver(requestObserver);
    when(requestObserver.isReady()).thenReturn(false);

    streamingBridge.onGrpcReady();

    verify(subscription, times(1)).request(1); // Only the initial one from onSubscribe
  }

  @Test
  void testOnGrpcReady_AlreadyCompleted_DoesNothing() {
    streamingBridge.onSubscribe(subscription);
    streamingBridge.setRequestObserver(requestObserver);
    streamingBridge.onComplete(); // sets completed = true

    streamingBridge.onGrpcReady();

    verify(subscription, times(1)).request(1); // Only the initial one
  }

  // --- ONNEXT TESTS ---

  @Test
  void testOnNext_Unary_SavesPayloadAndRequestsMore() {
    unaryBridge.onSubscribe(subscription);

    byte[] payload = "test-unary".getBytes();
    unaryBridge.onNext(createGrpcFrame(payload));

    // For Unary, it just saves the payload to singlePayload and requests more until EOF
    verify(subscription, times(2)).request(1);
  }

  @Test
  void testOnNext_Streaming_PassesToObserverAndRequestsMore_IfReady() {
    streamingBridge.onSubscribe(subscription);
    streamingBridge.setRequestObserver(requestObserver);
    when(requestObserver.isReady()).thenReturn(true);

    byte[] payload = "test-stream".getBytes();
    streamingBridge.onNext(createGrpcFrame(payload));

    // Verify it handed the payload directly to the observer
    ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
    verify(requestObserver).onNext(captor.capture());
    assertArrayEquals(payload, captor.getValue());

    // Verify it requested more because the observer was ready
    verify(subscription, times(2)).request(1);
  }

  @Test
  void testOnNext_Streaming_DoesNotRequestMore_IfNotReady() {
    streamingBridge.onSubscribe(subscription);
    streamingBridge.setRequestObserver(requestObserver);
    when(requestObserver.isReady()).thenReturn(false);

    byte[] payload = "test-stream".getBytes();
    streamingBridge.onNext(createGrpcFrame(payload));

    // Verify it handed payload over, but did NOT request more
    verify(requestObserver).onNext(any());
    verify(subscription, times(1)).request(1); // Only the initial one
  }

  @Test
  void testOnNext_ExceptionCaught_CancelsSubscriptionAndErrors() {
    streamingBridge.onSubscribe(subscription);
    streamingBridge.setRequestObserver(requestObserver);

    // Force an exception inside the processing callback
    RuntimeException simulatedError = new RuntimeException("Simulated processing error");
    doThrow(simulatedError).when(requestObserver).onNext(any());

    streamingBridge.onNext(createGrpcFrame("poison-pill".getBytes()));

    verify(subscription).cancel();
    verify(requestObserver).onError(simulatedError);
  }

  // --- ONERROR TESTS ---

  @Test
  void testOnError_WithRequestObserver() {
    streamingBridge.setRequestObserver(requestObserver);
    Throwable err = new Exception("Connection lost");

    streamingBridge.onError(err);

    verify(requestObserver).onError(err);
  }

  @Test
  void testOnError_WithResponseObserverOnly() {
    streamingBridge.setResponseObserver(responseObserver);
    Throwable err = new Exception("Connection lost");

    streamingBridge.onError(err);

    verify(responseObserver).onError(err);
  }

  @Test
  void testOnError_NoObservers_LogsOnly() {
    Throwable err = new Exception("Connection lost");
    unaryBridge.onError(err);
    // Should not throw NPE; completes gracefully by just logging
  }

  @Test
  void testOnError_AlreadyCompleted_IsIgnored() {
    streamingBridge.setRequestObserver(requestObserver);
    streamingBridge.onComplete(); // sets completed = true

    streamingBridge.onError(new Exception("Late error"));

    // Verify the error is ignored and not passed downstream
    verify(requestObserver, never()).onError(any());
  }

  // --- ONCOMPLETE TESTS ---

  @Test
  void testOnComplete_Unary_WithPayload() {
    unaryBridge.onSubscribe(subscription); // FIX: Satisfy the reactive streams lifecycle
    unaryBridge.setResponseObserver(responseObserver);
    byte[] payload = "unary-response".getBytes();
    unaryBridge.onNext(createGrpcFrame(payload)); // caches the payload

    try (MockedStatic<ClientCalls> clientCallsMock = mockStatic(ClientCalls.class)) {
      unaryBridge.onComplete();

      ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
      clientCallsMock.verify(
          () -> ClientCalls.asyncUnaryCall(eq(call), captor.capture(), eq(responseObserver)));
      assertArrayEquals(payload, captor.getValue());
    }
  }

  @Test
  void testOnComplete_Unary_EmptyPayload() {
    unaryBridge.setResponseObserver(responseObserver);
    // Calling complete without any onNext calls

    try (MockedStatic<ClientCalls> clientCallsMock = mockStatic(ClientCalls.class)) {
      unaryBridge.onComplete();

      ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
      clientCallsMock.verify(
          () -> ClientCalls.asyncUnaryCall(eq(call), captor.capture(), eq(responseObserver)));
      assertArrayEquals(new byte[0], captor.getValue()); // Fallback to empty array
    }
  }

  @Test
  void testOnComplete_ServerStreaming() {
    GrpcRequestBridge serverStreamBridge = new GrpcRequestBridge(call, MethodType.SERVER_STREAMING);
    serverStreamBridge.onSubscribe(subscription); // FIX: Satisfy the reactive streams lifecycle
    serverStreamBridge.setResponseObserver(responseObserver);
    byte[] payload = "req".getBytes();
    serverStreamBridge.onNext(createGrpcFrame(payload));

    try (MockedStatic<ClientCalls> clientCallsMock = mockStatic(ClientCalls.class)) {
      serverStreamBridge.onComplete();

      ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
      clientCallsMock.verify(
          () ->
              ClientCalls.asyncServerStreamingCall(
                  eq(call), captor.capture(), eq(responseObserver)));
      assertArrayEquals(payload, captor.getValue());
    }
  }

  @Test
  void testOnComplete_ClientOrBidiStreaming() {
    streamingBridge.setRequestObserver(requestObserver);

    streamingBridge.onComplete();

    verify(requestObserver).onCompleted();
  }

  @Test
  void testOnComplete_AlreadyCompleted_IsIgnored() {
    streamingBridge.setRequestObserver(requestObserver);

    streamingBridge.onComplete();
    streamingBridge.onComplete(); // Second call

    // Should only call onCompleted once
    verify(requestObserver, times(1)).onCompleted();
  }
}
