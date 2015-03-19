package org.jooby.internal;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.jooby.MediaType;
import org.jooby.Route;
import org.junit.Test;

public class RouteImplTest {

  @Test
  public void toStr() {
    Route route = new RouteImpl((req, rsp, chain) -> {
    }, "GET", "/path", "/p?th", "path", Collections.emptyMap(),
        MediaType.valueOf("html", "json"),
        MediaType.valueOf("json", "html"));

    assertEquals("GET /path\n" +
        "  pattern: /p?th\n" +
        "  name: path\n" +
        "  vars: {}\n" +
        "  consumes: [text/html, application/json]\n" +
        "  produces: [application/json, text/html]\n"
        , route.toString());
  }

  @Test
  public void consumes() {
    Route route = new RouteImpl((req, rsp, chain) -> {
    }, "GET", "/path", "/p?th", "path", Collections.emptyMap(),
        MediaType.valueOf("html", "json"),
        MediaType.valueOf("json", "html"));

    assertEquals(MediaType.valueOf("html", "json"), route.consumes());
  }

}
