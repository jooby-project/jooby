package io.jooby.json;

import io.jooby.MockContext;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JacksonTest {

  @Test
  public void render() throws Exception {
    Jackson jackson = new Jackson();
    MockContext ctx = new MockContext();
    byte [] bytes = jackson.encode(ctx, mapOf("k", "v"));
    assertEquals("{\"k\":\"v\"}", new String(bytes, StandardCharsets.UTF_8));
    /** Type: */
    assertEquals("application/json", ctx.responseContentType().value());
    assertEquals("utf-8", ctx.getResponseCharset().name().toLowerCase());
  }

  @Test
  public void parse() throws Exception {
    Jackson jackson = new Jackson();
    MockContext ctx = new MockContext();
    ctx.setBody("{\"k\":\"v\"}".getBytes(StandardCharsets.UTF_8));

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
