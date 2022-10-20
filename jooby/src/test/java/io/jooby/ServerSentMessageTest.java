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

public class ServerSentMessageTest {

  @Test
  public void shouldFormatMessage() throws Exception {
    String data = "some";
    Context ctx = mock(Context.class);

    MessageEncoder encoder = mock(MessageEncoder.class);
    when(encoder.encode(ctx, data)).thenReturn(data.getBytes(StandardCharsets.UTF_8));

    Route route = mock(Route.class);
    when(route.getEncoder()).thenReturn(encoder);

    when(ctx.getRoute()).thenReturn(route);

    ServerSentMessage message = new ServerSentMessage(data);
    assertEquals("data:some\n\n", new String(message.toByteArray(ctx), StandardCharsets.UTF_8));
  }
}
