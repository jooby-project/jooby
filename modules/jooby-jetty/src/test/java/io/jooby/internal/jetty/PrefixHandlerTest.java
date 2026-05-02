/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.http.HttpURI;
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
class PrefixHandlerTest {

  @Mock Request request;
  @Mock Response response;
  @Mock Callback callback;
  @Mock HttpURI uri;

  @Mock Handler apiHandler;
  @Mock Handler rootHandler;
  @Mock Handler otherHandler;

  @BeforeEach
  void setup() {
    // Lenient stubbing for the URI path which is called in all handle() invocations
    when(request.getHttpURI()).thenReturn(uri);
  }

  @Test
  void testHandle_MatchesPrefix() throws Exception {
    List<Map.Entry<String, Handler>> mappings =
        Arrays.asList(Map.entry("/api", apiHandler), Map.entry("/", rootHandler));
    PrefixHandler handler = new PrefixHandler(mappings);

    when(uri.getPath()).thenReturn("/api/users/1");
    when(apiHandler.handle(request, response, callback)).thenReturn(true);

    boolean result = handler.handle(request, response, callback);

    assertTrue(result);
    verify(apiHandler).handle(request, response, callback);
    verify(rootHandler, never()).handle(any(), any(), any());
  }

  @Test
  void testHandle_NoPrefixMatch_FallsBackToExplicitRootHandler() throws Exception {
    List<Map.Entry<String, Handler>> mappings =
        Arrays.asList(
            Map.entry("/api", apiHandler),
            Map.entry("/assets", otherHandler),
            Map.entry("/", rootHandler));
    PrefixHandler handler = new PrefixHandler(mappings);

    when(uri.getPath()).thenReturn("/index.html");
    when(rootHandler.handle(request, response, callback)).thenReturn(false);

    boolean result = handler.handle(request, response, callback);

    assertFalse(result);
    verify(rootHandler).handle(request, response, callback);
    verify(apiHandler, never()).handle(any(), any(), any());
    verify(otherHandler, never()).handle(any(), any(), any());
  }

  @Test
  void testHandle_NoPrefixMatch_FallsBackToZeroIndexWhenNoRootIsProvided() throws Exception {
    // If no "/" mapping is found, the constructor defaults the index to 0
    List<Map.Entry<String, Handler>> mappings =
        Arrays.asList(Map.entry("/api", apiHandler), Map.entry("/assets", otherHandler));
    PrefixHandler handler = new PrefixHandler(mappings);

    when(uri.getPath()).thenReturn("/unknown-path");
    when(apiHandler.handle(request, response, callback)).thenReturn(true);

    boolean result = handler.handle(request, response, callback);

    assertTrue(result);

    // It should fall back to the handler at index 0 (apiHandler)
    verify(apiHandler).handle(request, response, callback);
    verify(otherHandler, never()).handle(any(), any(), any());
  }

  @Test
  void testConstructor_RootAtIndexZeroBreaksLoopEarly() throws Exception {
    List<Map.Entry<String, Handler>> mappings =
        Arrays.asList(Map.entry("/", rootHandler), Map.entry("/api", apiHandler));

    // The constructor will find "/" at index 0 and break immediately, securing coverage.
    PrefixHandler handler = new PrefixHandler(mappings);

    when(uri.getPath()).thenReturn("/api/health");

    when(rootHandler.handle(request, response, callback)).thenReturn(true);

    boolean result = handler.handle(request, response, callback);

    assertTrue(result);
    // Verify rootHandler intercepted it due to loop order
    verify(rootHandler).handle(request, response, callback);
    verify(apiHandler, never()).handle(any(), any(), any());
  }
}
