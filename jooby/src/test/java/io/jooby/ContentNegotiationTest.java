package io.jooby;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ContentNegotiationTest {

  @Test
  public void wildMatch() {
    Object value = new ContentNegotiation()
        .accept("application/json", () -> "json")
        .render("text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");

    assertEquals("json", value);
  }

  @Test
  public void shouldMatchMostSpecific() {
    Object value = new ContentNegotiation()
        .accept("text/html", () -> "html")
        .accept("application/json", () -> "json")
        .render("text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");

    assertEquals("html", value);
  }

  @Test
  public void shouldMatchMostSpecificNoOrder() {
    Object value = new ContentNegotiation()
        .accept("application/json", () -> "json")
        .accept("text/html", () -> "html")
        .render("text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");

    assertEquals("html", value);

    value = new ContentNegotiation()
        .accept("application/json", () -> "json")
        .accept("text/html", () -> "html")
        .render("*/*;q=0.8,application/xhtml+xml,application/xml;q=0.9,image/webp,text/html");

    assertEquals("html", value);

    value = new ContentNegotiation()
        .accept("application/json", () -> "json")
        .accept("text/html", () -> "html")
        .render(
            "*/*;q=0.8,application/xhtml+xml,application/xml;q=0.9,image/webp,application/json");

    assertEquals("json", value);
  }

  @Test
  public void shouldMatchFirstOnWild() {
    Object value = new ContentNegotiation()
        .accept("application/json", () -> "json")
        .accept("text/html", () -> "html")
        .render("*/*");

    assertEquals("json", value);

    value = new ContentNegotiation()
        .accept("text/html", () -> "html")
        .accept("application/json", () -> "json")
        .render("*/*");

    assertEquals("html", value);
  }

  @Test
  public void shouldResolveAs406() {
    try {
      new ContentNegotiation()
          .accept("application/json", () -> "json")
          .accept("text/html", () -> "html")
          .render("text/plain");
    } catch (Err x) {
      assertEquals(406, x.statusCode.value());
    }
  }

  @Test
  public void shouldUseWildSubtype() {
    Object value = new ContentNegotiation()
        .accept("application/json", () -> "json")
        .accept("text/html", () -> "html")
        .accept("text/*", () -> "plain")
        .render("text/plain");

    assertEquals("plain", value);
  }

  @Test
  public void shouldUseFallback() {
    Object value = new ContentNegotiation()
        .accept("application/json", () -> "json")
        .accept("text/html", () -> "html")
        .accept(() -> "plain")
        .render("text/plain");

    assertEquals("plain", value);
  }
}
