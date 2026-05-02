/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jooby.Context;
import io.jooby.WebSocket;
import io.jooby.WebSocketCloseStatus;
import io.jooby.output.Output;

class MockWebSocketTest {

  private Context ctx;
  private MockWebSocketConfigurer configurer;
  private MockWebSocket ws;

  @BeforeEach
  void setUp() {
    ctx = mock(Context.class);
    configurer = mock(MockWebSocketConfigurer.class);
    ws = new MockWebSocket(ctx, configurer);
  }

  @Test
  void testGetContext() {
    assertEquals(ctx, ws.getContext());
  }

  @Test
  void testGetSessions() {
    assertTrue(ws.getSessions().isEmpty());
  }

  @Test
  void testIsOpen() {
    assertTrue(ws.isOpen());
    ws.close(WebSocketCloseStatus.NORMAL);
    assertFalse(ws.isOpen());
  }

  @Test
  void testForEach() {
    ws.forEach(webSocket -> assertEquals(ws, webSocket));
  }

  @Test
  void testSendPingString() {
    WebSocket.WriteCallback callback = mock(WebSocket.WriteCallback.class);
    ws.sendPing("ping", callback);
    verify(configurer).fireClientMessage("ping");
    verify(callback).operationComplete(ws, null);
  }

  @Test
  void testSendPingByteBuffer() {
    WebSocket.WriteCallback callback = mock(WebSocket.WriteCallback.class);
    ByteBuffer buffer = ByteBuffer.wrap("ping".getBytes(StandardCharsets.UTF_8));
    ws.sendPing(buffer, callback);
    verify(configurer).fireClientMessage(buffer);
    verify(callback).operationComplete(ws, null);
  }

  @Test
  void testSendString() {
    WebSocket.WriteCallback callback = mock(WebSocket.WriteCallback.class);
    ws.send("message", callback);
    verify(configurer).fireClientMessage("message");
    verify(callback).operationComplete(ws, null);
  }

  @Test
  void testSendByteArray() {
    WebSocket.WriteCallback callback = mock(WebSocket.WriteCallback.class);
    byte[] message = "message".getBytes(StandardCharsets.UTF_8);
    ws.send(message, callback);
    verify(configurer).fireClientMessage(message);
    verify(callback).operationComplete(ws, null);
  }

  @Test
  void testSendByteBuffer() {
    WebSocket.WriteCallback callback = mock(WebSocket.WriteCallback.class);
    ByteBuffer buffer = ByteBuffer.wrap("message".getBytes(StandardCharsets.UTF_8));
    ws.send(buffer, callback);
    verify(configurer).fireClientMessage(buffer);
    verify(callback).operationComplete(ws, null);
  }

  @Test
  void testSendOutput() {
    WebSocket.WriteCallback callback = mock(WebSocket.WriteCallback.class);
    Output output = mock(Output.class);
    ws.send(output, callback);
    verify(configurer).fireClientMessage(output);
    verify(callback).operationComplete(ws, null);
  }

  @Test
  void testSendBinaryString() {
    WebSocket.WriteCallback callback = mock(WebSocket.WriteCallback.class);
    ws.sendBinary("binary", callback);
    verify(configurer).fireClientMessage("binary");
    verify(callback).operationComplete(ws, null);
  }

  @Test
  void testSendBinaryByteArray() {
    WebSocket.WriteCallback callback = mock(WebSocket.WriteCallback.class);
    byte[] message = "binary".getBytes(StandardCharsets.UTF_8);
    ws.sendBinary(message, callback);
    verify(configurer).fireClientMessage(message);
    verify(callback).operationComplete(ws, null);
  }

  @Test
  void testSendBinaryByteBuffer() {
    WebSocket.WriteCallback callback = mock(WebSocket.WriteCallback.class);
    ByteBuffer buffer = ByteBuffer.wrap("binary".getBytes(StandardCharsets.UTF_8));
    ws.sendBinary(buffer, callback);
    verify(configurer).fireClientMessage(buffer);
    verify(callback).operationComplete(ws, null);
  }

  @Test
  void testSendBinaryOutput() {
    WebSocket.WriteCallback callback = mock(WebSocket.WriteCallback.class);
    Output output = mock(Output.class);
    ws.sendBinary(output, callback);
    verify(configurer).fireClientMessage(output);
    verify(callback).operationComplete(ws, null);
  }

  @Test
  void testRender() {
    WebSocket.WriteCallback callback = mock(WebSocket.WriteCallback.class);
    Object value = new Object();
    ws.render(value, callback);
    verify(configurer).fireClientMessage(value);
    verify(callback).operationComplete(ws, null);
  }

  @Test
  void testRenderBinary() {
    WebSocket.WriteCallback callback = mock(WebSocket.WriteCallback.class);
    Object value = new Object();
    ws.renderBinary(value, callback);
    verify(configurer).fireClientMessage(value);
    verify(callback).operationComplete(ws, null);
  }

  @Test
  void testSendObjectWithoutCallback() {
    ws.send("message", null);
    verify(configurer).fireClientMessage("message");
  }

  @Test
  void testSendObjectOnClosedSocket() {
    ws.close(WebSocketCloseStatus.NORMAL);
    WebSocket.WriteCallback callback = mock(WebSocket.WriteCallback.class);
    ws.send("message", callback);
    verify(configurer, never()).fireClientMessage("message");
    verify(callback).operationComplete(any(WebSocket.class), any(IllegalStateException.class));
    verify(configurer).fireError(any(IllegalStateException.class));
  }

  @Test
  void testSendObjectErrorPropagatesFatalException() {
    Error fatalError = new OutOfMemoryError("Test Error");
    doThrow(fatalError).when(configurer).fireClientMessage("message");

    assertThrows(OutOfMemoryError.class, () -> ws.send("message", null));
    verify(configurer).fireError(fatalError);
  }

  @Test
  void testClose() {
    ws.close(WebSocketCloseStatus.GOING_AWAY);
    assertFalse(ws.isOpen());
    verify(configurer).fireClose(WebSocketCloseStatus.GOING_AWAY);
  }

  @Test
  void testCloseErrorHandling() {
    RuntimeException ex = new RuntimeException("Close Error");
    doThrow(ex).when(configurer).fireClose(WebSocketCloseStatus.NORMAL);

    ws.close(WebSocketCloseStatus.NORMAL);
    assertFalse(ws.isOpen());
    verify(configurer).fireError(ex);
  }
}
