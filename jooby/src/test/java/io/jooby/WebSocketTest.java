/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jooby.output.Output;

public class WebSocketTest {

  private WebSocket ws;
  private Context ctx;

  @BeforeEach
  void setUp() {
    // We mock the interface but allow default methods to be executed
    ws = mock(WebSocket.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
    ctx = mock(Context.class);
    when(ws.getContext()).thenReturn(ctx);
  }

  @Test
  void attributesAndContextDelegation() {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("foo", "bar");

    when(ctx.getAttributes()).thenReturn(attributes);
    when(ctx.getAttribute("foo")).thenReturn("bar");

    assertEquals(attributes, ws.getAttributes());
    assertEquals("bar", ws.attribute("foo"));

    ws.attribute("key", "value");
    verify(ctx).setAttribute("key", "value");
  }

  @Test
  void sendPingVariants() {
    // sendPing(String)
    ws.sendPing("ping");
    verify(ws).sendPing(eq("ping"), eq(WebSocket.WriteCallback.NOOP));

    // sendPing(byte[])
    byte[] bytes = "ping".getBytes();
    ws.sendPing(bytes);
    verify(ws).sendPing(any(ByteBuffer.class), eq(WebSocket.WriteCallback.NOOP));

    // sendPing(ByteBuffer)
    ByteBuffer buffer = ByteBuffer.wrap(bytes);
    ws.sendPing(buffer);
    verify(ws, times(2)).sendPing(eq(buffer), eq(WebSocket.WriteCallback.NOOP));
  }

  @Test
  void sendTextVariants() {
    // send(String)
    ws.send("text");
    verify(ws).send(eq("text"), eq(WebSocket.WriteCallback.NOOP));

    // send(byte[])
    byte[] bytes = "text".getBytes();
    ws.send(bytes);
    verify(ws).send(any(ByteBuffer.class), eq(WebSocket.WriteCallback.NOOP));

    // send(ByteBuffer)
    ByteBuffer buffer = ByteBuffer.wrap(bytes);
    ws.send(buffer);
    verify(ws, times(2)).send(eq(buffer), eq(WebSocket.WriteCallback.NOOP));

    // send(Output)
    Output output = mock(Output.class);
    ws.send(output);
    verify(ws).send(eq(output), eq(WebSocket.WriteCallback.NOOP));
  }

  @Test
  void sendBinaryVariants() {
    // sendBinary(String)
    ws.sendBinary("bin");
    verify(ws).sendBinary(eq("bin"), eq(WebSocket.WriteCallback.NOOP));

    // sendBinary(byte[])
    byte[] bytes = "bin".getBytes();
    ws.sendBinary(bytes);
    verify(ws).sendBinary(any(ByteBuffer.class), eq(WebSocket.WriteCallback.NOOP));

    // sendBinary(ByteBuffer)
    ByteBuffer buffer = ByteBuffer.wrap(bytes);
    ws.sendBinary(buffer);
    verify(ws, times(2)).sendBinary(eq(buffer), eq(WebSocket.WriteCallback.NOOP));

    // sendBinary(Output)
    Output output = mock(Output.class);
    ws.sendBinary(output);
    verify(ws).sendBinary(eq(output), eq(WebSocket.WriteCallback.NOOP));
  }

  @Test
  void renderVariants() {
    Object data = new Object();

    ws.render(data);
    verify(ws).render(eq(data), eq(WebSocket.WriteCallback.NOOP));

    ws.renderBinary(data);
    verify(ws).renderBinary(eq(data), eq(WebSocket.WriteCallback.NOOP));
  }

  @Test
  void closeVariants() {
    ws.close();
    verify(ws).close(WebSocketCloseStatus.NORMAL);
  }

  @Test
  void noopWriteCallback() {
    // The operationComplete method in NOOP is a lambda that does nothing.
    // This triggers the branch to ensure no exceptions occur during invocation.
    WebSocket.WriteCallback.NOOP.operationComplete(ws, null);
    WebSocket.WriteCallback.NOOP.operationComplete(ws, new Exception());
  }

  @Test
  void constants() {
    // Verify interface constants are accessible
    assertEquals(131072, WebSocket.MAX_BUFFER_SIZE);
  }
}
