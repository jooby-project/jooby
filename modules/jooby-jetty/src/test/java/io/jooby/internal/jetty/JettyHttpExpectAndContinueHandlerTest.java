/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpHeaderValue;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JettyHttpExpectAndContinueHandlerTest {

  @Mock Handler next;
  @Mock Request request;
  @Mock Response response;
  @Mock Callback callback;

  private JettyHttpExpectAndContinueHandler handler;

  @BeforeEach
  void setup() {
    handler = new JettyHttpExpectAndContinueHandler(next);
  }

  @Test
  void testHandle_WithExpectContinueHeader_WritesInterimAndDelegates() throws Exception {
    // Populate headers with Expect: 100-continue
    HttpFields.Mutable headers = HttpFields.build();
    headers.add(HttpHeader.EXPECT, HttpHeaderValue.CONTINUE.asString());

    when(request.getHeaders()).thenReturn(headers);

    // Stub the delegate handler to return true
    when(next.handle(request, response, callback)).thenReturn(true);

    boolean result = handler.handle(request, response, callback);

    // Verify it intercepted the header and wrote the interim 100 Continue status
    verify(response).writeInterim(HttpStatus.CONTINUE_100, HttpFields.EMPTY);

    // Verify it returned the result of the delegate handler
    assertTrue(result);
    verify(next).handle(request, response, callback);
  }

  @Test
  void testHandle_WithoutExpectContinueHeader_JustDelegates() throws Exception {
    // Empty headers
    HttpFields.Mutable headers = HttpFields.build();

    when(request.getHeaders()).thenReturn(headers);

    // Stub the delegate handler to return false
    when(next.handle(request, response, callback)).thenReturn(false);

    boolean result = handler.handle(request, response, callback);

    // Verify it did NOT attempt to write an interim response
    verify(response, never()).writeInterim(anyInt(), any(HttpFields.class));

    // Verify it returned the result of the delegate handler
    assertFalse(result);
    verify(next).handle(request, response, callback);
  }

  @Test
  void testHandle_WithDifferentExpectHeader_JustDelegates() throws Exception {
    // Populate headers with a different Expect value
    HttpFields.Mutable headers = HttpFields.build();
    headers.add(HttpHeader.EXPECT, "some-other-expectation");

    when(request.getHeaders()).thenReturn(headers);
    when(next.handle(request, response, callback)).thenReturn(true);

    boolean result = handler.handle(request, response, callback);

    // Verify it did NOT attempt to write an interim response
    verify(response, never()).writeInterim(anyInt(), any(HttpFields.class));

    // Verify execution continued
    assertTrue(result);
    verify(next).handle(request, response, callback);
  }
}
