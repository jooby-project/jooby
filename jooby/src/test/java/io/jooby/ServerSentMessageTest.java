package io.jooby;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
