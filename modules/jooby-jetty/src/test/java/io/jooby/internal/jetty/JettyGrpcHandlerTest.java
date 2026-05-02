/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.Flow;

import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.server.ConnectionMetaData;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import io.jooby.rpc.grpc.GrpcProcessor;

@ExtendWith(MockitoExtension.class)
class JettyGrpcHandlerTest {

  @Mock Handler next;
  @Mock GrpcProcessor processor;
  @Mock Request request;
  @Mock Response response;
  @Mock Callback callback;

  @Mock HttpURI uri;
  @Mock HttpFields requestHeaders;
  @Mock HttpFields.Mutable responseHeaders;
  @Mock ConnectionMetaData metaData;

  private JettyGrpcHandler handler;

  @BeforeEach
  void setup() {
    handler = new JettyGrpcHandler(next, processor);

    // Set up lenient boilerplate for the request properties that might be accessed
    lenient().when(request.getHttpURI()).thenReturn(uri);
    lenient().when(uri.getPath()).thenReturn("/io.grpc.Service/Method");

    lenient().when(request.getHeaders()).thenReturn(requestHeaders);
    lenient().when(response.getHeaders()).thenReturn(responseHeaders);

    lenient().when(request.getConnectionMetaData()).thenReturn(metaData);
  }

  @Test
  void testHandle_NotGrpcMethod_DelegatesToNext() throws Exception {
    when(processor.isGrpcMethod("/io.grpc.Service/Method")).thenReturn(false);
    when(requestHeaders.get("Content-Type")).thenReturn("application/grpc"); // Ignored
    when(next.handle(request, response, callback)).thenReturn(false);

    boolean result = handler.handle(request, response, callback);

    assertFalse(result);
    verify(next).handle(request, response, callback);
    verify(processor, never()).process(any());
  }

  @Test
  void testHandle_NullContentType_DelegatesToNext() throws Exception {
    when(processor.isGrpcMethod("/io.grpc.Service/Method")).thenReturn(true);
    when(requestHeaders.get("Content-Type")).thenReturn(null);
    when(next.handle(request, response, callback)).thenReturn(true);

    boolean result = handler.handle(request, response, callback);

    assertTrue(result);
    verify(next).handle(request, response, callback);
    verify(processor, never()).process(any());
  }

  @Test
  void testHandle_WrongContentType_DelegatesToNext() throws Exception {
    when(processor.isGrpcMethod("/io.grpc.Service/Method")).thenReturn(true);
    when(requestHeaders.get("Content-Type")).thenReturn("application/json");
    when(next.handle(request, response, callback)).thenReturn(true);

    boolean result = handler.handle(request, response, callback);

    assertTrue(result);
    verify(next).handle(request, response, callback);
    verify(processor, never()).process(any());
  }

  @Test
  void testHandle_ValidGrpc_Http1_ReturnsUpgradeRequired() throws Exception {
    when(processor.isGrpcMethod("/io.grpc.Service/Method")).thenReturn(true);
    when(requestHeaders.get("Content-Type")).thenReturn("application/grpc");

    // Simulating HTTP/1.1
    when(metaData.getProtocol()).thenReturn("HTTP/1.1");

    boolean result = handler.handle(request, response, callback);

    assertTrue(result); // Consumes the request
    verify(response).setStatus(426);
    verify(responseHeaders).put("Connection", "Upgrade");
    verify(responseHeaders).put("Upgrade", "h2c");
    verify(callback).succeeded();

    // Ensure it halts execution and does not delegate
    verify(next, never()).handle(any(), any(), any());
    verify(processor, never()).process(any());
  }

  @Test
  @SuppressWarnings("unchecked")
  void testHandle_ValidGrpc_Http2_StartsBridge() throws Exception {
    when(processor.isGrpcMethod("/io.grpc.Service/Method")).thenReturn(true);
    when(requestHeaders.get("Content-Type")).thenReturn("application/grpc+proto");

    // Simulating HTTP/2
    when(metaData.getProtocol()).thenReturn("HTTP/2.0");

    Flow.Subscriber mockSubscriber = mock(Flow.Subscriber.class);
    when(processor.process(any(JettyGrpcExchange.class))).thenReturn(mockSubscriber);

    // Intercept creation of JettyGrpcInputBridge to verify it gets started
    try (MockedConstruction<JettyGrpcInputBridge> bridgeMock =
        mockConstruction(JettyGrpcInputBridge.class)) {
      boolean result = handler.handle(request, response, callback);

      assertTrue(result);

      // Verify the exchange was constructed and the processor consumed it
      verify(processor).process(any(JettyGrpcExchange.class));

      // Verify the input bridge was instantiated and started
      assertEquals(1, bridgeMock.constructed().size());
      verify(bridgeMock.constructed().get(0)).start();
    }

    // Ensure it halts execution and does not delegate to standard routing
    verify(next, never()).handle(any(), any(), any());
  }
}
