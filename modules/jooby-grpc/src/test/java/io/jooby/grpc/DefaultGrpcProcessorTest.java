/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.grpc;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.Flow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.jooby.internal.grpc.DefaultGrpcProcessor;
import io.jooby.internal.grpc.GrpcRequestBridge;
import io.jooby.rpc.grpc.GrpcExchange;

public class DefaultGrpcProcessorTest {

  private ManagedChannel channel;
  private Map<String, MethodDescriptor<?, ?>> registry;
  private GrpcExchange exchange;
  private ClientCall<byte[], byte[]> call;
  private DefaultGrpcProcessor bridge;

  @BeforeEach
  @SuppressWarnings("unchecked")
  public void setUp() {
    channel = mock(ManagedChannel.class);
    registry = mock(Map.class);
    exchange = mock(GrpcExchange.class);
    call = mock(ClientCall.class);

    // The interceptor wraps the channel, but eventually delegates to the real one
    when(channel.newCall(any(MethodDescriptor.class), any(CallOptions.class))).thenReturn(call);

    bridge = new DefaultGrpcProcessor(registry);
    bridge.setChannel(channel);
  }

  @Test
  @DisplayName(
      "Should throw IllegalStateException if an unknown method bypasses the isGrpcMethod guard")
  public void shouldRejectUnknownMethod() {
    when(exchange.getRequestPath()).thenReturn("/unknown.Service/Method");
    when(registry.get("unknown.Service/Method")).thenReturn(null);

    // Assert that the bridge correctly identifies the illegal state
    IllegalStateException exception =
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalStateException.class, () -> bridge.process(exchange));

    // Ensure the exchange wasn't manipulated or closed, because the framework
    // should crash the thread instead of trying to gracefully close a gRPC stream.
    verify(exchange, org.mockito.Mockito.never()).close(anyInt(), any());
  }

  @Test
  @DisplayName("Should successfully bridge a valid Bidi-Streaming gRPC call")
  public void shouldProcessValidStreamingCall() {
    setupValidMethod("test.Chat/Stream", MethodDescriptor.MethodType.BIDI_STREAMING);

    Flow.Subscriber<ByteBuffer> subscriber = bridge.process(exchange);

    assertNotNull(subscriber);
    assertTrue(subscriber instanceof GrpcRequestBridge);

    // Verify call was actually created
    verify(channel).newCall(any(MethodDescriptor.class), any(CallOptions.class));
  }

  @Test
  @DisplayName("Should parse grpc-timeout header into CallOptions deadline")
  public void shouldParseGrpcTimeout() {
    setupValidMethod("test.Chat/TimeoutCall", MethodDescriptor.MethodType.UNARY);

    // 1000m = 1000 milliseconds
    when(exchange.getHeader("grpc-timeout")).thenReturn("1000m");

    bridge.process(exchange);

    ArgumentCaptor<CallOptions> optionsCaptor = ArgumentCaptor.forClass(CallOptions.class);
    verify(channel).newCall(any(MethodDescriptor.class), optionsCaptor.capture());

    CallOptions options = optionsCaptor.getValue();
    assertNotNull(options.getDeadline());
    assertTrue(options.getDeadline().isExpired() == false, "Deadline should be in the future");
  }

  @Test
  @DisplayName("Should correctly deframe and send gRPC payload to the client")
  public void shouldFrameAndSendResponsePayload() {
    setupValidMethod("test.Chat/Stream", MethodDescriptor.MethodType.BIDI_STREAMING);
    bridge.process(exchange);

    // Capture the internal listener that gRPC uses to push data back to us
    ArgumentCaptor<ClientCall.Listener<byte[]>> listenerCaptor =
        ArgumentCaptor.forClass(ClientCall.Listener.class);
    verify(call).start(listenerCaptor.capture(), any(Metadata.class));
    ClientCall.Listener<byte[]> responseListener = listenerCaptor.getValue();

    // Simulate the server pushing a payload back
    byte[] serverResponse = "hello".getBytes();
    responseListener.onMessage(serverResponse);

    // Verify our bridge framed it with the 5-byte header and sent it to Jooby
    ArgumentCaptor<ByteBuffer> bufferCaptor = ArgumentCaptor.forClass(ByteBuffer.class);
    verify(exchange).send(bufferCaptor.capture(), any());

    ByteBuffer framedBuffer = bufferCaptor.getValue();
    assertEquals(5 + serverResponse.length, framedBuffer.limit());
    assertEquals((byte) 0, framedBuffer.get(), "Compressed flag should be 0");
    assertEquals(serverResponse.length, framedBuffer.getInt(), "Length should match payload");

    byte[] capturedPayload = new byte[serverResponse.length];
    framedBuffer.get(capturedPayload);
    assertArrayEquals(serverResponse, capturedPayload);
  }

  @Test
  @DisplayName("Should close exchange with HTTP/2 trailing status when server throws error")
  public void shouldCloseExchangeOnError() {
    setupValidMethod("test.Chat/Stream", MethodDescriptor.MethodType.BIDI_STREAMING);
    bridge.process(exchange);

    ArgumentCaptor<ClientCall.Listener<byte[]>> listenerCaptor =
        ArgumentCaptor.forClass(ClientCall.Listener.class);
    verify(call).start(listenerCaptor.capture(), any(Metadata.class));
    ClientCall.Listener<byte[]> responseListener = listenerCaptor.getValue();

    // Simulate an internal server error from the gRPC engine
    Status errorStatus = Status.INVALID_ARGUMENT.withDescription("Bad data");
    responseListener.onClose(errorStatus, new Metadata());

    // Verify it mapped down to the core exchange SPI
    verify(exchange).close(errorStatus.getCode().value(), "Bad data");
  }

  @Test
  @DisplayName("Should gracefully close exchange with Status 0 on completion")
  public void shouldCloseExchangeOnComplete() {
    setupValidMethod("test.Chat/Stream", MethodDescriptor.MethodType.BIDI_STREAMING);
    bridge.process(exchange);

    ArgumentCaptor<ClientCall.Listener<byte[]>> listenerCaptor =
        ArgumentCaptor.forClass(ClientCall.Listener.class);
    verify(call).start(listenerCaptor.capture(), any(Metadata.class));
    ClientCall.Listener<byte[]> responseListener = listenerCaptor.getValue();

    // Simulate clean completion
    responseListener.onClose(Status.OK, new Metadata());

    verify(exchange).close(Status.OK.getCode().value(), null);
  }

  @Test
  @DisplayName("Should extract and decode custom metadata headers")
  public void shouldExtractMetadata() {
    // CRITICAL FIX: Use BIDI_STREAMING instead of UNARY.
    // Unary delays call.start() until the request stream completes. Bidi starts immediately.
    setupValidMethod("test.Chat/Headers", MethodDescriptor.MethodType.BIDI_STREAMING);

    Map<String, String> incomingHeaders =
        Map.of(
            "x-custom-id",
            "12345",
            "x-custom-bin",
            Base64.getEncoder().encodeToString("binary_data".getBytes()));
    when(exchange.getHeaders()).thenReturn(incomingHeaders);

    bridge.process(exchange);

    // Verify that the metadata was extracted and attached to the call
    ArgumentCaptor<Metadata> metadataCaptor = ArgumentCaptor.forClass(Metadata.class);
    verify(call).start(any(), metadataCaptor.capture());

    Metadata metadata = metadataCaptor.getValue();

    assertEquals(
        "12345", metadata.get(Metadata.Key.of("x-custom-id", Metadata.ASCII_STRING_MARSHALLER)));
    assertArrayEquals(
        "binary_data".getBytes(),
        metadata.get(Metadata.Key.of("x-custom-bin", Metadata.BINARY_BYTE_MARSHALLER)));
  }

  /** Helper to mock out a valid MethodDescriptor in the registry. */
  @SuppressWarnings("unchecked")
  private void setupValidMethod(String methodPath, MethodDescriptor.MethodType type) {
    MethodDescriptor<byte[], byte[]> descriptor =
        MethodDescriptor.<byte[], byte[]>newBuilder()
            .setType(type)
            .setFullMethodName(methodPath)
            .setRequestMarshaller(mock(MethodDescriptor.Marshaller.class))
            .setResponseMarshaller(mock(MethodDescriptor.Marshaller.class))
            .build();

    when(exchange.getRequestPath()).thenReturn("/" + methodPath);
    //noinspection rawtypes
    when(registry.get(methodPath)).thenReturn((MethodDescriptor) descriptor);
  }
}
