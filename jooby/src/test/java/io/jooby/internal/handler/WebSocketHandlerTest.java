/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jooby.Context;
import io.jooby.StatusCode;
import io.jooby.WebSocket;
import io.jooby.value.Value;

class WebSocketHandlerTest {

  private Context ctx;
  private WebSocket.Initializer initializer;
  private Value upgradeHeader;

  @BeforeEach
  void setUp() {
    ctx = mock(Context.class);
    initializer = mock(WebSocket.Initializer.class);
    upgradeHeader = mock(Value.class);

    when(ctx.header("Upgrade")).thenReturn(upgradeHeader);
  }

  @Test
  @DisplayName("Verify getInitializer returns the provided handler")
  void testGetInitializer() {
    WebSocketHandler handler = new WebSocketHandler(initializer);
    assertEquals(initializer, handler.getInitializer());
  }

  @Test
  @DisplayName("Verify successful WebSocket upgrade returns context")
  void testApplySuccessfulUpgrade() {
    when(upgradeHeader.value("")).thenReturn("WebSocket");
    // Simulate that the upgrade successfully started the response
    when(ctx.isResponseStarted()).thenReturn(true);

    WebSocketHandler handler = new WebSocketHandler(initializer);
    Object result = handler.apply(ctx);

    // Verify upgrade was called and the context was returned natively
    verify(ctx).upgrade(initializer);
    assertEquals(ctx, result);
    verify(ctx, never()).send(any(StatusCode.class));
  }

  @Test
  @DisplayName("Verify non-WebSocket request returns NOT_FOUND")
  void testApplyNonWebSocketRequest() {
    when(upgradeHeader.value("")).thenReturn("keep-alive");
    when(ctx.isResponseStarted()).thenReturn(false);

    // Mock the send behavior
    Context errorContext = mock(Context.class);
    when(ctx.send(StatusCode.NOT_FOUND)).thenReturn(errorContext);

    WebSocketHandler handler = new WebSocketHandler(initializer);
    Object result = handler.apply(ctx);

    // Verify upgrade was NEVER called
    verify(ctx, never()).upgrade(any(WebSocket.Initializer.class));

    // Verify 404 was sent and returned
    verify(ctx).send(StatusCode.NOT_FOUND);
    assertEquals(errorContext, result);
  }

  @Test
  @DisplayName("Verify upgrade called but response not started falls back to NOT_FOUND")
  void testApplyUpgradeFailsToStartResponse() {
    // Branch condition 1: True (Is a WebSocket request)
    when(upgradeHeader.value("")).thenReturn("websocket"); // testing case-insensitivity too

    // Branch condition 2: False (Response somehow didn't start)
    when(ctx.isResponseStarted()).thenReturn(false);

    Context errorContext = mock(Context.class);
    when(ctx.send(StatusCode.NOT_FOUND)).thenReturn(errorContext);

    WebSocketHandler handler = new WebSocketHandler(initializer);
    Object result = handler.apply(ctx);

    // Verify it attempted to upgrade
    verify(ctx).upgrade(initializer);

    // Verify it still fell back to NOT_FOUND because response wasn't started
    verify(ctx).send(StatusCode.NOT_FOUND);
    assertEquals(errorContext, result);
  }
}
