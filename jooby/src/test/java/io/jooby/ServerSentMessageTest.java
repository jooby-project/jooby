/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import io.jooby.buffer.DefaultDataBufferFactory;

public class ServerSentMessageTest {

  @Test
  public void shouldFormatMessage() throws Exception {
    String data = "some";
    Context ctx = mock(Context.class);

    var bufferFactory = new DefaultDataBufferFactory();
    when(ctx.getBufferFactory()).thenReturn(bufferFactory);
    MessageEncoder encoder = mock(MessageEncoder.class);
    when(encoder.encode(ctx, data))
        .thenReturn(bufferFactory.wrap(data.getBytes(StandardCharsets.UTF_8)));

    Route route = mock(Route.class);
    when(route.getEncoder()).thenReturn(encoder);

    when(ctx.getRoute()).thenReturn(route);

    ServerSentMessage message = new ServerSentMessage(data);
    assertEquals(
        "data:" + data + "\n\n", message.toByteArray(ctx).toString(StandardCharsets.UTF_8));
  }

  @Test
  public void shouldFormatMultiLineMessage() throws Exception {
    String data = "line 1\n line ,a .. 2\nline ...abc  3";
    Context ctx = mock(Context.class);

    var bufferFactory = new DefaultDataBufferFactory();
    when(ctx.getBufferFactory()).thenReturn(bufferFactory);
    MessageEncoder encoder = mock(MessageEncoder.class);
    when(encoder.encode(ctx, data))
        .thenReturn(bufferFactory.wrap(data.getBytes(StandardCharsets.UTF_8)));

    Route route = mock(Route.class);
    when(route.getEncoder()).thenReturn(encoder);

    when(ctx.getRoute()).thenReturn(route);

    ServerSentMessage message = new ServerSentMessage(data);
    assertEquals(
        "data:line 1\ndata: line ,a .. 2\ndata:line ...abc  3\n\n",
        message.toByteArray(ctx).toString(StandardCharsets.UTF_8));
  }

  @Test
  public void shouldFormatMessageEndingWithNL() throws Exception {
    String data = "line 1\n";
    Context ctx = mock(Context.class);

    var bufferFactory = new DefaultDataBufferFactory();
    when(ctx.getBufferFactory()).thenReturn(bufferFactory);
    MessageEncoder encoder = mock(MessageEncoder.class);
    when(encoder.encode(ctx, data))
        .thenReturn(bufferFactory.wrap(data.getBytes(StandardCharsets.UTF_8)));

    Route route = mock(Route.class);
    when(route.getEncoder()).thenReturn(encoder);

    when(ctx.getRoute()).thenReturn(route);

    ServerSentMessage message = new ServerSentMessage(data);
    assertEquals(
        "data:" + data + "\n\n", message.toByteArray(ctx).toString(StandardCharsets.UTF_8));
  }
}
