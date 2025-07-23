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

import io.jooby.output.OutputFactory;
import io.jooby.output.OutputOptions;

public class ServerSentMessageTest {

  @Test
  public void shouldFormatMessage() throws Exception {
    var data = "some";
    var ctx = mock(Context.class);

    var bufferFactory = OutputFactory.create(OutputOptions.small());
    when(ctx.getOutputFactory()).thenReturn(bufferFactory);
    var encoder = mock(MessageEncoder.class);
    when(encoder.encode(ctx, data))
        .thenReturn(bufferFactory.wrap(data.getBytes(StandardCharsets.UTF_8)));

    var route = mock(Route.class);
    when(route.getEncoder()).thenReturn(encoder);

    when(ctx.getRoute()).thenReturn(route);

    var message = new ServerSentMessage(data);
    assertEquals(
        "data: " + data + "\n\n",
        StandardCharsets.UTF_8.decode(message.encode(ctx).asByteBuffer()).toString());
  }

  @Test
  public void shouldFormatMultiLineMessage() throws Exception {
    var data = "line 1\n line ,a .. 2\nline ...abc  3";
    var ctx = mock(Context.class);

    var bufferFactory = OutputFactory.create(OutputOptions.small());
    when(ctx.getOutputFactory()).thenReturn(bufferFactory);
    var encoder = mock(MessageEncoder.class);
    when(encoder.encode(ctx, data))
        .thenReturn(bufferFactory.wrap(data.getBytes(StandardCharsets.UTF_8)));

    var route = mock(Route.class);
    when(route.getEncoder()).thenReturn(encoder);

    when(ctx.getRoute()).thenReturn(route);

    var message = new ServerSentMessage(data);
    assertEquals(
        "data: line 1\ndata:  line ,a .. 2\ndata: line ...abc  3\n\n",
        StandardCharsets.UTF_8.decode(message.encode(ctx).asByteBuffer()).toString());
  }

  @Test
  public void shouldFormatMessageEndingWithNL() throws Exception {
    var data = "line 1\n";
    var ctx = mock(Context.class);

    var bufferFactory = OutputFactory.create(OutputOptions.small());
    when(ctx.getOutputFactory()).thenReturn(bufferFactory);
    var encoder = mock(MessageEncoder.class);
    when(encoder.encode(ctx, data))
        .thenReturn(bufferFactory.wrap(data.getBytes(StandardCharsets.UTF_8)));

    var route = mock(Route.class);
    when(route.getEncoder()).thenReturn(encoder);

    when(ctx.getRoute()).thenReturn(route);

    var message = new ServerSentMessage(data);
    assertEquals(
        "data: " + data + "\n\n",
        StandardCharsets.UTF_8.decode(message.encode(ctx).asByteBuffer()).toString());
  }
}
