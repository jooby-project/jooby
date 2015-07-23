package org.jooby.internal;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.jooby.Route;
import org.jooby.WebSocket;
import org.junit.Test;

import com.google.common.collect.Sets;

public class AppPrinterTest {

  @Test
  public void print() {
    String setup = new AppPrinter(
        Sets.newLinkedHashSet(Arrays.asList(route("/"), route("/home"))),
        Sets.newLinkedHashSet(Arrays.asList(socket("/ws"))), "localhost", 8080, "/")
        .toString();
    assertEquals("  GET /        [*/*]     [*/*]    (anonymous)\n" +
        "  GET /home    [*/*]     [*/*]    (anonymous)\n" +
        "  WS  /ws      [*/*]     [*/*]\n" +
        "\n" +
        "listening on:\n" +
        "  http://localhost:8080/", setup);
  }

  @Test
  public void printWithPath() {
    String setup = new AppPrinter(
        Sets.newLinkedHashSet(Arrays.asList(route("/"), route("/home"))),
        Sets.newLinkedHashSet(Arrays.asList(socket("/ws"))), "localhost", 8080, "/app")
        .toString();
    assertEquals("  GET /        [*/*]     [*/*]    (anonymous)\n" +
        "  GET /home    [*/*]     [*/*]    (anonymous)\n" +
        "  WS  /ws      [*/*]     [*/*]\n" +
        "\n" +
        "listening on:\n" +
        "  http://localhost:8080/app", setup);
  }

  @Test
  public void printNoSockets() {
    String setup = new AppPrinter(
        Sets.newLinkedHashSet(Arrays.asList(route("/"), route("/home"))),
        Sets.newLinkedHashSet(), "localhost", 8080, "/app")
        .toString();
    assertEquals("  GET /        [*/*]     [*/*]    (anonymous)\n" +
        "  GET /home    [*/*]     [*/*]    (anonymous)\n" +
        "\n" +
        "listening on:\n" +
        "  http://localhost:8080/app", setup);
  }


  private Route.Definition route(final String pattern) {
    return new Route.Definition("GET", pattern, (req, rsp) -> {
    });
  }

  private WebSocket.Definition socket(final String pattern) {
    return new WebSocket.Definition(pattern, (ws) -> {
    });
  }
}
