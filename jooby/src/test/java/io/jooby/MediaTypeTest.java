package io.jooby;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MediaTypeTest {

  @Test
  public void json() {
    MediaType type = MediaType.json;
    assertEquals("application/json", type.toString());
    assertEquals("application/json", type.value());
    assertEquals("application", type.type());
    assertEquals("json", type.subtype());
    assertEquals(1f, type.quality());
    assertEquals("UTF-8", type.charset());
  }

  @Test
  public void text() {
    MediaType type = MediaType.text;
    assertEquals("text/plain", type.toString());
    assertEquals("text/plain", type.value());
    assertEquals("text", type.type());
    assertEquals("plain", type.subtype());
    assertEquals(1f, type.quality());
    assertEquals("UTF-8", type.charset());

    assertEquals("text/plain", MediaType.valueOf("text/plain").value());

    assertEquals("text/plain", MediaType.valueOf("text/plain;charset=UTF-8").value());
  }

  @Test
  public void html() {
    MediaType type = MediaType.html;
    assertEquals("text/html", type.toString());
    assertEquals("text/html", type.value());
    assertEquals("text", type.type());
    assertEquals("html", type.subtype());
    assertEquals(1f, type.quality());
    assertEquals("UTF-8", type.charset());
  }

  @Test
  public void valueOf() {
    MediaType type = MediaType.valueOf("application / json; q=0.5; charset=us-ascii");
    assertEquals("application / json; q=0.5; charset=us-ascii", type.toString());
    assertEquals("application / json", type.value());
    assertEquals("application", type.type());
    assertEquals("json", type.subtype());
    assertEquals(.5f, type.quality());
    assertEquals("us-ascii", type.charset());

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

  @Test
  public void matches() {
    assertTrue(MediaType.matches("application/json", "text/html, */*"));

    assertTrue(MediaType.matches("application/json", "application/json"));

    assertTrue(MediaType.matches("application/*+json", "application/xml, application/bar+json "));

    assertFalse(MediaType.matches("application/json", "text/plain"));

    assertFalse(MediaType.matches("application/json", "text/plain"));

    assertTrue(MediaType.matches("application/json", "*"));
    assertTrue(MediaType.matches("application/json", "*/*"));


    assertFalse(MediaType.matches("application/json", "application/jsonx"));
    assertFalse(MediaType.matches("application/json", "application/xjson"));

    // wild
    assertTrue(MediaType.matches("application/*json", "application/json"));
    assertTrue(MediaType.matches("application/*+json", "application/foo+json"));

    assertTrue(MediaType.matches("application/*json", "application/foojson"));
    assertFalse(MediaType.matches("application/*+json", "application/foojson"));
    assertFalse(MediaType.matches("application/*+json", "text/plain"));
    assertFalse(MediaType.matches("application/*+json", "application/jsonplain"));

    // accept header
    assertTrue(MediaType.matches("application/json", "application/json, application/xml"));

    assertTrue(MediaType.matches("application/json", "application/xml, application/json"));

    assertTrue(MediaType.matches("application/*+json", "application/xml, application/bar+json"));

    assertTrue(MediaType.matches("application/json", "application/json, application/xml"));


  }
}
