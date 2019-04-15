package io.jooby.json;

import io.jooby.Body;
import io.jooby.Context;
import io.jooby.MediaType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JacksonTest {

  @Test
  public void render() throws Exception {
    Context ctx = mock(Context.class);

    Jackson jackson = new Jackson();

    byte[] bytes = jackson.render(ctx, mapOf("k", "v"));
    assertEquals("{\"k\":\"v\"}", new String(bytes, StandardCharsets.UTF_8));

    verify(ctx).setDefaultResponseType(MediaType.json);
  }

  @Test
  public void parse() throws Exception {
    byte[] bytes = "{\"k\":\"v\"}".getBytes(StandardCharsets.UTF_8);
    Body body = mock(Body.class);
    when(body.isInMemory()).thenReturn(true);
    when(body.bytes()).thenReturn(bytes);

    Context ctx = mock(Context.class);
    when(ctx.body()).thenReturn(body);

    Jackson jackson = new Jackson();

    Map<String, String> result = jackson.parse(ctx, Map.class);
    assertEquals(mapOf("k", "v"), result);
  }

  private Map<String, String> mapOf(String... values) {
    Map<String, String> hash = new HashMap<>();
    for (int i = 0; i < values.length; i += 2) {
      hash.put(values[i], values[i + 1]);
    }
    return hash;
  }
}
