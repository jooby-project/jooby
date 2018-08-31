package io.jooby;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MediaTypeTest {

  @Test
  public void json() {
    MediaType json = MediaType.json;
    assertEquals("application/json", json.toString());
    assertEquals("application/json", json.value());
    assertEquals("application", json.type());
    assertEquals("json", json.subtype());
    assertEquals(1f, json.quality());
    assertEquals(StandardCharsets.UTF_8, json.charset());
  }

  @Test
  public void text() {
    MediaType type = MediaType.text;
    assertEquals("text/plain", type.toString());
    assertEquals("text/plain", type.value());
    assertEquals("text", type.type());
    assertEquals("plain", type.subtype());
    assertEquals(1f, type.quality());
    assertEquals(StandardCharsets.UTF_8, type.charset());
  }

  @Test
  public void html() {
    MediaType type = MediaType.html;
    assertEquals("text/html", type.toString());
    assertEquals("text/html", type.value());
    assertEquals("text", type.type());
    assertEquals("html", type.subtype());
    assertEquals(1f, type.quality());
    assertEquals(StandardCharsets.UTF_8, type.charset());
  }

  @Test
  public void valueOf() {
    MediaType json = MediaType.valueOf("application / json; q=0.5; charset=us-ascii");
    assertEquals("application / json; q=0.5; charset=us-ascii", json.toString());
    assertEquals("application / json; q=0.5; charset=us-ascii", json.value());
    assertEquals("application", json.type());
    assertEquals("json", json.subtype());
    assertEquals(.5f, json.quality());
    assertEquals(StandardCharsets.US_ASCII, json.charset());

    MediaType any = MediaType.valueOf("*");
    assertEquals("*/*", any.value());
    assertEquals("*", any.type());
    assertEquals("*", any.subtype());

    any = MediaType.valueOf("");
    assertEquals("*/*", any.value());
    assertEquals("*", any.type());
    assertEquals("*", any.subtype());

    any = MediaType.valueOf(null);
    assertEquals("*/*", any.value());
    assertEquals("*", any.type());
    assertEquals("*", any.subtype());
  }

  @Test
  public void parse() {
    List<MediaType> result = MediaType.parse("application/json , text/html,*");
    assertEquals(3, result.size());
    assertEquals("application/json", result.get(0).value());
    assertEquals("text/html", result.get(1).value());
    assertEquals("*/*", result.get(2).value());

    assertEquals(0, MediaType.parse(null).size());
    assertEquals(0, MediaType.parse("").size());
    assertEquals(1, MediaType.parse("text/plain,").size());
  }
}
