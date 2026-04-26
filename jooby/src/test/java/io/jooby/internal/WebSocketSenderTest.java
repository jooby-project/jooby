/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.mockito.Mockito.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jooby.*;
import io.jooby.output.Output;

public class WebSocketSenderTest {

  private Context ctx;
  private WebSocket ws;
  private WebSocket.WriteCallback callback;

  @BeforeEach
  void setUp() {
    ctx = mock(Context.class);
    ws = mock(WebSocket.class);
    callback = mock(WebSocket.WriteCallback.class);
  }

  @Test
  void testSendTextMode() {
    WebSocketSender sender = new WebSocketSender(ctx, ws, false, callback);
    byte[] data = "hello".getBytes(StandardCharsets.UTF_8);
    ByteBuffer buffer = ByteBuffer.wrap(data);
    Output output = mock(Output.class);

    sender.send("hello", StandardCharsets.UTF_8);
    verify(ws).send(data, callback);

    sender.send(data);
    verify(ws, times(2)).send(data, callback);

    sender.send(buffer);
    verify(ws).send(buffer, callback);

    sender.send(output);
    verify(ws).send(output, callback);
  }

  @Test
  void testSendBinaryMode() {
    WebSocketSender sender = new WebSocketSender(ctx, ws, true, callback);
    byte[] data = "binary".getBytes(StandardCharsets.UTF_8);
    ByteBuffer buffer = ByteBuffer.wrap(data);
    Output output = mock(Output.class);

    sender.send("binary", StandardCharsets.UTF_8);
    verify(ws).sendBinary(data, callback);

    sender.send(data);
    verify(ws, times(2)).sendBinary(data, callback);

    sender.send(buffer);
    verify(ws).sendBinary(buffer, callback);

    sender.send(output);
    verify(ws).sendBinary(output, callback);
  }

  @Test
  void testNoopMethods() {
    WebSocketSender sender = new WebSocketSender(ctx, ws, false, callback);

    // All these should do nothing (NOOP) and return 'this'
    sender
        .setResetHeadersOnError(true)
        .setDefaultResponseType(MediaType.json)
        .setResponseCode(200)
        .setResponseCode(StatusCode.OK)
        .setResponseCookie(new Cookie("test"))
        .setResponseHeader("name", "value")
        .setResponseHeader("name", new Date())
        .setResponseHeader("name", Instant.now())
        .setResponseHeader("name", new Object())
        .setResponseLength(100)
        .setResponseType("text/plain")
        .setResponseType(MediaType.text);

    // Verify no interactions with the underlying context for these specific methods
    verifyNoInteractions(ctx);
  }

  @Test
  void testRender() throws Exception {
    WebSocketSender sender = new WebSocketSender(ctx, ws, false, callback);

    // Setup mocks for the render pipeline
    Route route = mock(Route.class);
    MessageEncoder encoder = mock(MessageEncoder.class);
    Object value = "render-me";
    var output = mock(Output.class);

    when(ctx.getRoute()).thenReturn(route);
    when(route.getEncoder()).thenReturn(encoder);
    // Stub encoder to return bytes so it triggers the send(byte[]) logic
    when(encoder.encode(sender, value)).thenReturn(output);

    sender.render(value);

    // Verify that render eventually called ws.send because binary was false
    verify(ws).send(output, callback);
  }

  @Test
  void testRenderBinary() throws Exception {
    WebSocketSender sender = new WebSocketSender(ctx, ws, true, callback);

    Route route = mock(Route.class);
    MessageEncoder encoder = mock(MessageEncoder.class);
    Object value = "render-me";
    var output = mock(Output.class);

    when(ctx.getRoute()).thenReturn(route);
    when(route.getEncoder()).thenReturn(encoder);
    when(encoder.encode(sender, value)).thenReturn(output);

    sender.render(value);

    // Verify that render eventually called ws.sendBinary because binary was true
    verify(ws).sendBinary(output, callback);
  }
}
