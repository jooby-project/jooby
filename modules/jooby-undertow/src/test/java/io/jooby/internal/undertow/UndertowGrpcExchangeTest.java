/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.undertow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.xnio.ChannelListener;
import org.xnio.channels.StreamSinkChannel;
import org.xnio.channels.StreamSourceChannel;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.protocol.http.HttpAttachments;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;

@ExtendWith(MockitoExtension.class)
class UndertowGrpcExchangeTest {

  @Mock HttpServerExchange exchange;
  @Mock StreamSinkChannel responseChannel;
  @Mock StreamSourceChannel requestChannel;
  @Mock Consumer<Throwable> callback;
  @Mock ChannelListener.Setter writeSetter;

  private HeaderMap requestHeaders;
  private HeaderMap responseHeaders;
  private UndertowGrpcExchange grpcExchange;

  @BeforeEach
  void setup() {
    requestHeaders = new HeaderMap();
    responseHeaders = new HeaderMap();

    lenient().when(exchange.getRequestHeaders()).thenReturn(requestHeaders);
    lenient().when(exchange.getResponseHeaders()).thenReturn(responseHeaders);
    lenient().when(exchange.getResponseChannel()).thenReturn(responseChannel);
    lenient().when(exchange.getRequestChannel()).thenReturn(requestChannel);
    lenient().when(responseChannel.getWriteSetter()).thenReturn(writeSetter);

    grpcExchange = new UndertowGrpcExchange(exchange);
  }

  @Test
  void testGetRequestPath() {
    when(exchange.getRequestPath()).thenReturn("/io.grpc.Service/Method");
    assertEquals("/io.grpc.Service/Method", grpcExchange.getRequestPath());
  }

  @Test
  void testGetHeader() {
    requestHeaders.put(HttpString.tryFromString("User-Agent"), "grpc-java");
    assertEquals("grpc-java", grpcExchange.getHeader("User-Agent"));
    assertNull(grpcExchange.getHeader("Missing-Header"));
  }

  @Test
  void testGetHeaders() {
    requestHeaders.put(HttpString.tryFromString("Content-Type"), "application/grpc");
    requestHeaders.put(HttpString.tryFromString("te"), "trailers");

    Map<String, String> headers = grpcExchange.getHeaders();
    assertEquals(2, headers.size());
    assertEquals("application/grpc", headers.get("Content-Type"));
    assertEquals("trailers", headers.get("te"));
  }

  @Test
  void testSend_InitialWriteAndFlushSuccessful() throws Exception {
    ByteBuffer payload = ByteBuffer.allocate(10);
    // Write fully
    when(responseChannel.write(payload))
        .thenAnswer(
            inv -> {
              payload.position(10);
              return 10;
            });
    when(responseChannel.flush()).thenReturn(true);

    grpcExchange.send(payload, callback);

    assertEquals("application/grpc", responseHeaders.getFirst(Headers.CONTENT_TYPE));
    verify(callback).accept(null);
  }

  @Test
  void testSend_InitialWriteThrowsException() throws Exception {
    ByteBuffer payload = ByteBuffer.allocate(10);
    IOException ioException = new IOException("Socket write failed");
    when(responseChannel.write(payload)).thenThrow(ioException);

    grpcExchange.send(payload, callback);

    verify(callback).accept(ioException);
  }

  @Test
  @SuppressWarnings("unchecked")
  void testSend_PartialWrite_CompletesInListener() throws Exception {
    ByteBuffer payload = ByteBuffer.allocate(10);

    // Initial partial write
    when(responseChannel.write(payload))
        .thenAnswer(
            inv -> {
              payload.position(5);
              return 5;
            });

    grpcExchange.send(payload, callback);

    ArgumentCaptor<ChannelListener<StreamSinkChannel>> captor =
        ArgumentCaptor.forClass(ChannelListener.class);
    verify(writeSetter).set(captor.capture());
    verify(responseChannel).resumeWrites();

    // Trigger the listener, write fully this time
    when(responseChannel.write(payload))
        .thenAnswer(
            inv -> {
              payload.position(10);
              return 5;
            });
    when(responseChannel.flush()).thenReturn(true);

    captor.getValue().handleEvent(responseChannel);

    verify(responseChannel).suspendWrites();
    verify(callback).accept(null);
  }

  @Test
  @SuppressWarnings("unchecked")
  void testSend_PartialWrite_ListenerThrowsException() throws Exception {
    ByteBuffer payload = ByteBuffer.allocate(10);

    // Initial partial write
    when(responseChannel.write(payload))
        .thenAnswer(
            inv -> {
              payload.position(5);
              return 5;
            });

    grpcExchange.send(payload, callback);

    ArgumentCaptor<ChannelListener<StreamSinkChannel>> captor =
        ArgumentCaptor.forClass(ChannelListener.class);
    verify(writeSetter).set(captor.capture());

    // Throw exception in listener
    IOException ioException = new IOException("Listener write failed");
    when(responseChannel.write(payload)).thenThrow(ioException);

    captor.getValue().handleEvent(responseChannel);

    verify(responseChannel).suspendWrites();
    verify(callback).accept(ioException);
  }

  @Test
  void testSend_FlushThrowsException() throws Exception {
    ByteBuffer payload = ByteBuffer.allocate(0); // Already consumed
    when(responseChannel.write(payload)).thenReturn(0);

    IOException ioException = new IOException("Flush failed");
    when(responseChannel.flush()).thenThrow(ioException);

    grpcExchange.send(payload, callback);

    verify(callback).accept(ioException);
  }

  @Test
  @SuppressWarnings("unchecked")
  void testSend_PartialFlush_CompletesInListener() throws Exception {
    ByteBuffer payload = ByteBuffer.allocate(0); // Already consumed
    when(responseChannel.write(payload)).thenReturn(0);
    when(responseChannel.flush()).thenReturn(false);

    grpcExchange.send(payload, callback);

    ArgumentCaptor<ChannelListener<StreamSinkChannel>> captor =
        ArgumentCaptor.forClass(ChannelListener.class);
    verify(writeSetter).set(captor.capture());
    verify(responseChannel).resumeWrites();

    // Trigger listener and complete flush
    when(responseChannel.flush()).thenReturn(true);
    captor.getValue().handleEvent(responseChannel);

    verify(responseChannel).suspendWrites();
    verify(callback).accept(null);
  }

  @Test
  @SuppressWarnings("unchecked")
  void testSend_PartialFlush_ListenerThrowsException() throws Exception {
    ByteBuffer payload = ByteBuffer.allocate(0); // Already consumed
    when(responseChannel.write(payload)).thenReturn(0);
    when(responseChannel.flush()).thenReturn(false);

    grpcExchange.send(payload, callback);

    ArgumentCaptor<ChannelListener<StreamSinkChannel>> captor =
        ArgumentCaptor.forClass(ChannelListener.class);
    verify(writeSetter).set(captor.capture());

    // Trigger listener and throw exception
    IOException ioException = new IOException("Listener flush failed");
    when(responseChannel.flush()).thenThrow(ioException);

    captor.getValue().handleEvent(responseChannel);

    verify(responseChannel).suspendWrites();
    verify(callback).accept(ioException);
  }

  @Test
  void testClose_HeadersNotSent_NoDescription() {
    grpcExchange.close(0, null);

    assertEquals("application/grpc", responseHeaders.getFirst(Headers.CONTENT_TYPE));
    assertEquals("0", responseHeaders.getFirst("grpc-status"));
    assertNull(responseHeaders.getFirst("grpc-message"));
    verify(exchange).endExchange();
  }

  @Test
  void testClose_HeadersNotSent_WithDescription() {
    grpcExchange.close(1, "Not Found");

    assertEquals("application/grpc", responseHeaders.getFirst(Headers.CONTENT_TYPE));
    assertEquals("1", responseHeaders.getFirst("grpc-status"));
    assertEquals("Not Found", responseHeaders.getFirst("grpc-message"));
    verify(exchange).endExchange();
  }

  @Test
  @SuppressWarnings("unchecked")
  void testClose_HeadersAlreadySent_FlushImmediate() throws Exception {
    // Force headersSent to true
    when(responseChannel.write(any(ByteBuffer.class))).thenReturn(0);
    when(responseChannel.flush()).thenReturn(true);
    grpcExchange.send(ByteBuffer.allocate(0), callback);

    when(responseChannel.flush()).thenReturn(true);

    grpcExchange.close(0, null);

    ArgumentCaptor<Supplier<HeaderMap>> supplierCaptor = ArgumentCaptor.forClass(Supplier.class);
    verify(exchange)
        .putAttachment(eq(HttpAttachments.RESPONSE_TRAILER_SUPPLIER), supplierCaptor.capture());

    // Verify trailer evaluation
    HeaderMap trailers = supplierCaptor.getValue().get();
    assertEquals("0", trailers.getFirst("grpc-status"));
    assertNull(trailers.getFirst("grpc-message"));

    verify(responseChannel).shutdownWrites();
    verify(requestChannel).close(); // IoUtils.safeClose inner verification
    verify(responseChannel).close();
  }

  @Test
  @SuppressWarnings("unchecked")
  void testClose_HeadersAlreadySent_WithDescription_And_FlushDeferred() throws Exception {
    // Force headersSent to true
    when(responseChannel.write(any(ByteBuffer.class))).thenReturn(0);
    when(responseChannel.flush()).thenReturn(true);
    grpcExchange.send(ByteBuffer.allocate(0), callback);

    when(responseChannel.flush()).thenReturn(false);

    grpcExchange.close(1, "Detailed error");

    ArgumentCaptor<Supplier<HeaderMap>> supplierCaptor = ArgumentCaptor.forClass(Supplier.class);
    verify(exchange)
        .putAttachment(eq(HttpAttachments.RESPONSE_TRAILER_SUPPLIER), supplierCaptor.capture());

    // Verify trailer evaluation
    HeaderMap trailers = supplierCaptor.getValue().get();
    assertEquals("1", trailers.getFirst("grpc-status"));
    assertEquals("Detailed error", trailers.getFirst("grpc-message"));

    // Verify listener logic
    ArgumentCaptor<ChannelListener<StreamSinkChannel>> captor =
        ArgumentCaptor.forClass(ChannelListener.class);
    verify(writeSetter).set(captor.capture());
    verify(responseChannel).resumeWrites();

    // Trigger deferred flush success
    when(responseChannel.flush()).thenReturn(true);
    captor.getValue().handleEvent(responseChannel);

    verify(responseChannel).suspendWrites();
    verify(requestChannel).close();
    verify(responseChannel).close();
  }

  @Test
  @SuppressWarnings("unchecked")
  void testClose_HeadersAlreadySent_FlushDeferred_ListenerThrowsException() throws Exception {
    // Force headersSent to true
    when(responseChannel.write(any(ByteBuffer.class))).thenReturn(0);
    when(responseChannel.flush()).thenReturn(true);
    grpcExchange.send(ByteBuffer.allocate(0), callback);

    when(responseChannel.flush()).thenReturn(false);

    grpcExchange.close(0, null);

    ArgumentCaptor<ChannelListener<StreamSinkChannel>> captor =
        ArgumentCaptor.forClass(ChannelListener.class);
    verify(writeSetter).set(captor.capture());

    // Trigger deferred flush exception
    when(responseChannel.flush()).thenThrow(new IOException("Channel died during flush"));
    captor.getValue().handleEvent(responseChannel);

    verify(responseChannel).suspendWrites();
    verify(requestChannel).close();
    verify(responseChannel).close();
  }

  @Test
  void testClose_HeadersAlreadySent_ShutdownThrowsException() throws Exception {
    // Force headersSent to true
    when(responseChannel.write(any(ByteBuffer.class))).thenReturn(0);
    when(responseChannel.flush()).thenReturn(true);
    grpcExchange.send(ByteBuffer.allocate(0), callback);

    // Initial shutdown throws
    doThrow(new IOException("Shutdown failed")).when(responseChannel).shutdownWrites();

    grpcExchange.close(0, null);

    // Even if it throws, it must trigger safeClose
    verify(requestChannel).close();
    verify(responseChannel).close();
  }
}
