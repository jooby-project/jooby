/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JettyGrpcExchangeTest {

  @Mock Request request;
  @Mock Response response;
  @Mock Callback jettyCallback;
  @Mock Consumer<Throwable> joobyCallback;

  private HttpFields.Mutable requestHeaders;
  private HttpFields.Mutable responseHeaders;
  private JettyGrpcExchange exchange;

  @BeforeEach
  void setup() {
    requestHeaders = HttpFields.build();
    responseHeaders = HttpFields.build();

    // Use lenient() to prevent UnnecessaryStubbingExceptions in tests that don't read headers
    lenient().when(request.getHeaders()).thenReturn(requestHeaders);
    lenient().when(response.getHeaders()).thenReturn(responseHeaders);

    exchange = new JettyGrpcExchange(request, response, jettyCallback);
  }

  @Test
  void testConstructorSetsContentType() {
    assertEquals("application/grpc", responseHeaders.get("Content-Type"));
  }

  @Test
  void testGetRequestPath() {
    HttpURI uri = mock(HttpURI.class);
    when(request.getHttpURI()).thenReturn(uri);
    when(uri.getPath()).thenReturn("/io.grpc.Service/Method");

    assertEquals("/io.grpc.Service/Method", exchange.getRequestPath());
  }

  @Test
  void testGetHeader() {
    requestHeaders.put("User-Agent", "grpc-java-netty/1.0");

    assertEquals("grpc-java-netty/1.0", exchange.getHeader("User-Agent"));
    assertNull(exchange.getHeader("Missing"));
  }

  @Test
  void testGetHeaders() {
    requestHeaders.put("Content-Type", "application/grpc");
    requestHeaders.put("te", "trailers");

    Map<String, String> headers = exchange.getHeaders();

    assertEquals(2, headers.size());
    assertEquals("application/grpc", headers.get("Content-Type"));
    assertEquals("trailers", headers.get("te"));
  }

  @Test
  void testSend_SuccessAndFailureCallbacks() {
    ByteBuffer payload = ByteBuffer.allocate(10);

    exchange.send(payload, joobyCallback);

    ArgumentCaptor<Callback> callbackCaptor = ArgumentCaptor.forClass(Callback.class);
    verify(response).write(eq(false), eq(payload), callbackCaptor.capture());

    Callback capturedJettyCallback = callbackCaptor.getValue();

    // Trigger success
    capturedJettyCallback.succeeded();
    verify(joobyCallback).accept(null);

    // Trigger failure
    Throwable error = new RuntimeException("Network Error");
    capturedJettyCallback.failed(error);
    verify(joobyCallback).accept(error);
  }

  @Test
  @SuppressWarnings("unchecked")
  void testClose_HeadersSent_WithDescription() {
    ArgumentCaptor<Supplier<HttpFields>> supplierCaptor = ArgumentCaptor.forClass(Supplier.class);
    verify(response).setTrailersSupplier(supplierCaptor.capture());

    // Trigger headersSent = true
    exchange.send(ByteBuffer.allocate(0), joobyCallback);

    // Close the stream with a status and description
    exchange.close(14, "Unavailable");

    HttpFields trailers = supplierCaptor.getValue().get();
    assertEquals("14", trailers.get("grpc-status"));
    assertEquals("Unavailable", trailers.get("grpc-message"));

    verify(response).write(eq(true), any(ByteBuffer.class), eq(jettyCallback));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testClose_HeadersSent_NoDescription() {
    ArgumentCaptor<Supplier<HttpFields>> supplierCaptor = ArgumentCaptor.forClass(Supplier.class);
    verify(response).setTrailersSupplier(supplierCaptor.capture());

    exchange.send(ByteBuffer.allocate(0), joobyCallback);

    exchange.close(0, null);

    HttpFields trailers = supplierCaptor.getValue().get();
    assertEquals("0", trailers.get("grpc-status"));
    assertNull(trailers.get("grpc-message"));

    verify(response).write(eq(true), any(ByteBuffer.class), eq(jettyCallback));
  }

  @Test
  void testClose_HeadersNotSent_WithDescription() {
    exchange.close(2, "Unknown Error");

    assertEquals("2", responseHeaders.get("grpc-status"));
    assertEquals("Unknown Error", responseHeaders.get("grpc-message"));
    assertEquals("application/grpc", responseHeaders.get("Content-Type"));

    verify(response).write(eq(true), any(ByteBuffer.class), eq(jettyCallback));
  }

  @Test
  void testClose_HeadersNotSent_NoDescription() {
    exchange.close(0, null);

    assertEquals("0", responseHeaders.get("grpc-status"));
    assertNull(responseHeaders.get("grpc-message"));

    verify(response).write(eq(true), any(ByteBuffer.class), eq(jettyCallback));
  }
}
