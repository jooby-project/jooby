/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.undertow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.Flow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import io.jooby.rpc.grpc.GrpcProcessor;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.Protocols;

@ExtendWith(MockitoExtension.class)
class UndertowGrpcHandlerTest {

  @Mock HttpHandler next;
  @Mock GrpcProcessor processor;
  @Mock HttpServerExchange exchange;

  private HeaderMap requestHeaders;
  private HeaderMap responseHeaders;
  private UndertowGrpcHandler handler;

  @BeforeEach
  void setup() {
    requestHeaders = new HeaderMap();
    responseHeaders = new HeaderMap();

    lenient().when(exchange.getRequestHeaders()).thenReturn(requestHeaders);
    lenient().when(exchange.getResponseHeaders()).thenReturn(responseHeaders);

    handler = new UndertowGrpcHandler(next, processor);
  }

  @Test
  void testHandleRequest_NotGrpcMethod_PassesDownPipeline() throws Exception {
    when(exchange.getRequestPath()).thenReturn("/api/rest");
    when(processor.isGrpcMethod("/api/rest")).thenReturn(false);

    handler.handleRequest(exchange);

    verify(next).handleRequest(exchange);
    verify(processor, never()).process(any());
  }

  @Test
  void testHandleRequest_IsGrpcMethod_NullContentType_PassesDownPipeline() throws Exception {
    when(exchange.getRequestPath()).thenReturn("/io.grpc.Service/Method");
    when(processor.isGrpcMethod("/io.grpc.Service/Method")).thenReturn(true);
    // requestHeaders is empty, so CONTENT_TYPE is null

    handler.handleRequest(exchange);

    verify(next).handleRequest(exchange);
    verify(processor, never()).process(any());
  }

  @Test
  void testHandleRequest_IsGrpcMethod_WrongContentType_PassesDownPipeline() throws Exception {
    when(exchange.getRequestPath()).thenReturn("/io.grpc.Service/Method");
    when(processor.isGrpcMethod("/io.grpc.Service/Method")).thenReturn(true);
    requestHeaders.put(Headers.CONTENT_TYPE, "application/json");

    handler.handleRequest(exchange);

    verify(next).handleRequest(exchange);
    verify(processor, never()).process(any());
  }

  @Test
  void testHandleRequest_ValidGrpc_Http11_ReturnsUpgradeRequired() throws Exception {
    when(exchange.getRequestPath()).thenReturn("/io.grpc.Service/Method");
    when(processor.isGrpcMethod("/io.grpc.Service/Method")).thenReturn(true);
    requestHeaders.put(Headers.CONTENT_TYPE, "application/grpc");

    // Simulating HTTP/1.1
    when(exchange.getProtocol()).thenReturn(Protocols.HTTP_1_1);

    handler.handleRequest(exchange);

    verify(exchange).setStatusCode(426);
    assertEquals("Upgrade", responseHeaders.getFirst(Headers.CONNECTION));
    assertEquals("h2c", responseHeaders.getFirst(Headers.UPGRADE));
    verify(exchange).endExchange();

    // Ensure pipeline is halted
    verify(next, never()).handleRequest(any());
    verify(processor, never()).process(any());
  }

  @Test
  @SuppressWarnings("unchecked")
  void testHandleRequest_ValidGrpc_Http2_AcceptsAndStartsBridge() throws Exception {
    when(exchange.getRequestPath()).thenReturn("/io.grpc.Service/Method");
    when(processor.isGrpcMethod("/io.grpc.Service/Method")).thenReturn(true);
    requestHeaders.put(Headers.CONTENT_TYPE, "application/grpc+proto");

    // Simulating HTTP/2
    when(exchange.getProtocol()).thenReturn(Protocols.HTTP_2_0);

    Flow.Subscriber mockSubscriber = mock(Flow.Subscriber.class);
    when(processor.process(any(UndertowGrpcExchange.class))).thenReturn(mockSubscriber);

    // Intercept creation of UndertowGrpcInputBridge to verify it gets started
    try (MockedConstruction<UndertowGrpcInputBridge> bridgeMock =
        mockConstruction(UndertowGrpcInputBridge.class)) {
      handler.handleRequest(exchange);

      // Verify the exchange was constructed and the processor consumed it
      verify(processor).process(any(UndertowGrpcExchange.class));

      // Verify the input bridge was instantiated and started
      assertEquals(1, bridgeMock.constructed().size());
      verify(bridgeMock.constructed().get(0)).start();
    }

    // Ensure pipeline is halted
    verify(next, never()).handleRequest(any());
  }
}
