package io.jooby.json;

import io.jooby.MockContext;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JacksonTest {

  @Test
  public void render() throws Exception {
    Jackson jackson = new Jackson();
    MockContext ctx = new MockContext();
    boolean renderered = jackson.render(ctx, Map.of("k", "v"));
    assertTrue(renderered);

    /** Body: */
    byte[] bytes = (byte[]) ctx.getResult();
    assertEquals("{\"k\":\"v\"}", new String(bytes, StandardCharsets.UTF_8));
    /** Type: */
    assertEquals("application/json", ctx.getResponseContentType().value());
    assertEquals("utf-8", ctx.getResponseCharset().name().toLowerCase());
  }

  @Test
  public void renderRawString() throws Exception {
    Jackson jackson = new Jackson();
    MockContext ctx = new MockContext();
    boolean renderered = jackson.render(ctx, "{\"k\":\"v\"}");
    assertTrue(renderered);

    /** Body: */
    assertEquals("{\"k\":\"v\"}", ctx.getResultText());
    /** Type: */
    assertEquals("application/json", ctx.getResponseContentType().value());
    assertEquals("utf-8", ctx.getResponseCharset().name().toLowerCase());
  }

  @Test
  public void parse() throws Exception {
    Jackson jackson = new Jackson();
    MockContext ctx = new MockContext();
    ctx.setBody("{\"k\":\"v\"}".getBytes(StandardCharsets.UTF_8));

    Map<String, String> result = jackson.parse(ctx, Map.class);
    assertEquals(Map.of("k", "v"), result);
  }
}
