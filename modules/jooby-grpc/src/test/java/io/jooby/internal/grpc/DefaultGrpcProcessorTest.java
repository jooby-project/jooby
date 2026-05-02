/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.grpc;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.ClientResponseObserver;
import io.jooby.rpc.grpc.GrpcExchange;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class DefaultGrpcProcessorTest {

  @Mock ManagedChannel channel;
  @Mock GrpcExchange exchange;
  @Mock ClientCall clientCall;

  private Map<String, MethodDescriptor<?, ?>> registry;
  private DefaultGrpcProcessor processor;

  @BeforeEach
  void setup() {
    registry = new HashMap<>();

    // Register a Unary method
    MethodDescriptor<String, String> unaryMethod =
        MethodDescriptor.<String, String>newBuilder()
            .setType(MethodDescriptor.MethodType.UNARY)
            .setFullMethodName("pkg.Svc/Unary")
            .setRequestMarshaller(mock(MethodDescriptor.Marshaller.class))
            .setResponseMarshaller(mock(MethodDescriptor.Marshaller.class))
            .build();
    registry.put("pkg.Svc/Unary", unaryMethod);

    // Register a Bidi Streaming method
    MethodDescriptor<String, String> bidiMethod =
        MethodDescriptor.<String, String>newBuilder()
            .setType(MethodDescriptor.MethodType.BIDI_STREAMING)
            .setFullMethodName("pkg.Svc/Bidi")
            .setRequestMarshaller(mock(MethodDescriptor.Marshaller.class))
            .setResponseMarshaller(mock(MethodDescriptor.Marshaller.class))
            .build();
    registry.put("pkg.Svc/Bidi", bidiMethod);

    processor = new DefaultGrpcProcessor(registry);
    processor.setChannel(channel);

    lenient().when(channel.newCall(any(), any())).thenReturn(clientCall);
  }

  // --- REGISTRY & PATH GUARD TESTS ---

  @Test
  void testIsGrpcMethod() {
    assertTrue(processor.isGrpcMethod("/pkg.Svc/Unary"));
    assertTrue(processor.isGrpcMethod("pkg.Svc/Unary"));
    assertFalse(processor.isGrpcMethod("/pkg.Svc/Unknown"));
  }

  @Test
  void testProcess_UnregisteredMethod_ThrowsException() {
    when(exchange.getRequestPath()).thenReturn("/pkg.Svc/Unknown");

    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> processor.process(exchange));
    assertTrue(ex.getMessage().contains("Unregistered gRPC method"));
  }

  // --- METADATA & TIMEOUT EXTRACTION TESTS ---

  @Test
  void testProcess_ExtractMetadata_FiltersAndDecodesHeaders() {
    when(exchange.getRequestPath()).thenReturn("/pkg.Svc/Unary");

    Map<String, String> headers = new HashMap<>();
    headers.put(":method", "POST");
    headers.put("grpc-timeout", "1H");
    headers.put("content-type", "application/grpc");
    headers.put("te", "trailers");
    headers.put("authorization", "Bearer token");
    headers.put("custom-bin", Base64.getEncoder().encodeToString("binary-data".getBytes()));

    when(exchange.getHeaders()).thenReturn(headers);

    processor.process(exchange);

    verify(channel).newCall(any(), any());
  }

  @Test
  void testProcess_ExtractCallOptions_AllTimeUnitsAndInvalid() {
    when(exchange.getRequestPath()).thenReturn("/pkg.Svc/Unary");

    List<String> timeouts =
        List.of("1H", "2M", "3S", "4m", "5u", "6n", "10X", "invalid-format", "");

    for (String timeout : timeouts) {
      when(exchange.getHeader("grpc-timeout")).thenReturn(timeout);
      processor.process(exchange);
    }

    verify(channel, times(timeouts.size())).newCall(any(), any());
  }

  // --- MARSHALLER TESTS ---

  @Test
  void testRawMarshaller_StreamAndParse() throws Exception {
    when(exchange.getRequestPath()).thenReturn("/pkg.Svc/Unary");
    processor.process(exchange);

    ArgumentCaptor<MethodDescriptor> methodCaptor = ArgumentCaptor.forClass(MethodDescriptor.class);
    verify(channel).newCall(methodCaptor.capture(), any());

    MethodDescriptor.Marshaller<byte[]> marshaller = methodCaptor.getValue().getRequestMarshaller();

    // Test Stream
    byte[] testData = {1, 2, 3};
    InputStream stream = marshaller.stream(testData);

    // Test Parse (Happy Path)
    byte[] parsed = marshaller.parse(stream);
    assertArrayEquals(testData, parsed);

    // Test Parse (IOException)
    InputStream faultyStream =
        new InputStream() {
          @Override
          public int read() throws IOException {
            throw new IOException("Simulated Read Error");
          }
        };

    RuntimeException ex =
        assertThrows(RuntimeException.class, () -> marshaller.parse(faultyStream));
    assertEquals(IOException.class, ex.getCause().getClass());
  }

  // --- STREAM OBSERVER BEHAVIOR TESTS ---

  @Test
  void testObserver_Unary_BeforeStart_BypassesReadiness() throws Exception {
    when(exchange.getRequestPath()).thenReturn("/pkg.Svc/Unary");

    Flow.Subscriber<ByteBuffer> subscriber = processor.process(exchange);
    ClientResponseObserver observer = extractObserver(subscriber);

    ClientCallStreamObserver streamObserver = mock(ClientCallStreamObserver.class);
    observer.beforeStart(streamObserver);

    verify(streamObserver, never()).setOnReadyHandler(any());
  }

  @Test
  void testObserver_BidiStreaming_Lifecycle() throws Exception {
    when(exchange.getRequestPath()).thenReturn("/pkg.Svc/Bidi");

    try (MockedStatic<ClientCalls> clientCalls = mockStatic(ClientCalls.class)) {
      Flow.Subscriber<ByteBuffer> subscriber = processor.process(exchange);

      clientCalls.verify(() -> ClientCalls.asyncBidiStreamingCall(any(ClientCall.class), any()));

      ClientResponseObserver observer = extractObserver(subscriber);
      ClientCallStreamObserver streamObserver = mock(ClientCallStreamObserver.class);

      // 1. Test beforeStart (wires readiness)
      observer.beforeStart(streamObserver);
      verify(streamObserver).setOnReadyHandler(any());

      // 2. Test onNext (frames data and sends to exchange)
      byte[] payload = {10, 20};
      observer.onNext(payload);

      ArgumentCaptor<ByteBuffer> bufferCaptor = ArgumentCaptor.forClass(ByteBuffer.class);

      verify(exchange).send(bufferCaptor.capture(), any());

      // Safely extract the callback manually from the invocation history
      Object callback =
          org.mockito.Mockito.mockingDetails(exchange).getInvocations().stream()
              .filter(inv -> inv.getMethod().getName().equals("send"))
              .reduce((first, second) -> second) // get the most recent invocation
              .get()
              .getArgument(1);

      // Validate gRPC header framing
      ByteBuffer framed = bufferCaptor.getValue();
      assertEquals(0, framed.get());
      assertEquals(2, framed.getInt());
      assertEquals(10, framed.get());
      assertEquals(20, framed.get());

      // 3. Test onNext Callback Success (does nothing)
      invokeLambda(callback, null);
      verify(exchange, never()).close(anyInt(), anyString());

      // 4. Test onNext Callback Error (closes exchange with error status)
      // FIX: Use proper Status Exception to avoid message stripping
      Throwable simulatedError =
          Status.UNKNOWN.withDescription("Write timeout").asRuntimeException();
      invokeLambda(callback, simulatedError);

      verify(exchange).close(Status.UNKNOWN.getCode().value(), "Write timeout");

      // Since it errored, it is marked as finished. Subsequent calls should be ignored.
      clearInvocations(exchange);
      observer.onCompleted();
      verify(exchange, never()).close(anyInt(), any());
    }
  }

  @Test
  void testObserver_OnCompleted_ClosesExchangeWithOK() throws Exception {
    when(exchange.getRequestPath()).thenReturn("/pkg.Svc/Unary");

    Flow.Subscriber<ByteBuffer> subscriber = processor.process(exchange);
    ClientResponseObserver observer = extractObserver(subscriber);

    observer.onCompleted();

    verify(exchange).close(Status.OK.getCode().value(), null);

    // Verify state lockout
    clearInvocations(exchange);
    observer.onNext(new byte[] {1});
    verify(exchange, never()).send(any(), any());
  }

  @Test
  void testObserver_OnError_ClosesExchangeWithError() throws Exception {
    when(exchange.getRequestPath()).thenReturn("/pkg.Svc/Unary");

    Flow.Subscriber<ByteBuffer> subscriber = processor.process(exchange);
    ClientResponseObserver observer = extractObserver(subscriber);

    Throwable err = Status.INVALID_ARGUMENT.withDescription("Bad argument").asRuntimeException();
    observer.onError(err);

    verify(exchange).close(Status.INVALID_ARGUMENT.getCode().value(), "Bad argument");

    // Verify state lockout
    clearInvocations(exchange);
    observer.onError(new RuntimeException("Late error"));
    verify(exchange, never()).close(anyInt(), anyString());
  }

  // --- HELPERS ---

  /** Universal helper to invoke a Single Abstract Method (SAM) callback interface safely. */
  private void invokeLambda(Object callback, Throwable cause) throws Exception {
    if (callback instanceof java.util.function.Consumer) {
      ((java.util.function.Consumer) callback).accept(cause);
    } else {
      // Reflection fallback for internal Jooby functional interfaces
      for (java.lang.reflect.Method method : callback.getClass().getMethods()) {
        if (method.getParameterCount() == 1 && method.getDeclaringClass() != Object.class) {
          method.setAccessible(true);
          method.invoke(callback, cause);
          return;
        }
      }
      throw new IllegalStateException(
          "Could not find SAM method on callback: " + callback.getClass());
    }
  }

  /** Extracts the anonymous ClientResponseObserver created inside DefaultGrpcProcessor */
  private ClientResponseObserver<byte[], byte[]> extractObserver(
      Flow.Subscriber<ByteBuffer> subscriber) throws Exception {
    java.lang.reflect.Field field = subscriber.getClass().getDeclaredField("responseObserver");
    field.setAccessible(true);
    return (ClientResponseObserver<byte[], byte[]>) field.get(subscriber);
  }
}
