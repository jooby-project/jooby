package org.jooby;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class WebSocketDefinitionTest {

  @Test
  public void toStr() {
    WebSocket.Definition def = new WebSocket.Definition("/pattern", (ws) -> {
    });

    assertEquals("WS /pattern\n" +
        "  consume: */*\n" +
        "  produces: */*\n", def.toString());
  }

  @Test
  public void matches() {
    WebSocket.Definition def = new WebSocket.Definition("/pattern", (ws) -> {
    });

    assertEquals(true, def.matches("/pattern").isPresent());
    assertEquals(false, def.matches("/patter").isPresent());
  }

  @Test
  public void consumes() {
    assertEquals(MediaType.json, new WebSocket.Definition("/pattern", (ws) -> {
    }).consumes("json").consumes());
  }

  @Test(expected = NullPointerException.class)
  public void consumesNull() {
    new WebSocket.Definition("/pattern", (ws) -> {
    }).consumes((MediaType) null);

  }

  @Test
  public void produces() {
    assertEquals(MediaType.json, new WebSocket.Definition("/pattern", (ws) -> {
    }).produces("json").produces());
  }

  @Test(expected = NullPointerException.class)
  public void producesNull() {
    new WebSocket.Definition("/pattern", (ws) -> {
    }).produces((MediaType) null);
  }

  @Test
  public void identity() {
    assertEquals(
        new WebSocket.Definition("/pattern", (ws) -> {
        }),
        new WebSocket.Definition("/pattern", (ws) -> {
        }));

    assertEquals(
        new WebSocket.Definition("/pattern", (ws) -> {
        }).hashCode(),
        new WebSocket.Definition("/pattern", (ws) -> {
        }).hashCode());

    assertNotEquals(
        new WebSocket.Definition("/path", (ws) -> {
        }),
        new WebSocket.Definition("/patternx", (ws) -> {
        }));

    assertNotEquals(
        new WebSocket.Definition("/patternx", (ws) -> {
        }),
        new Object());
  }

}
