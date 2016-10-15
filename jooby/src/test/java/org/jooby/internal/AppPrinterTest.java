package org.jooby.internal;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.jooby.Route;
import org.jooby.Route.Before;
import org.jooby.WebSocket;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class AppPrinterTest {

  @Test
  public void print() {
    String setup = new AppPrinter(
        Sets.newLinkedHashSet(
            Arrays.asList(before("/"), beforeSend("/"), after("/"), route("/"), route("/home"))),
        Sets.newLinkedHashSet(Arrays.asList(socket("/ws"))), config("/"))
            .toString();
    assertEquals("  GET {before}/      [*/*]     [*/*]    (/anonymous)\n" +
        "  GET {after}/       [*/*]     [*/*]    (/anonymous)\n" +
        "  GET {complete}/    [*/*]     [*/*]    (/anonymous)\n" +
        "  GET /              [*/*]     [*/*]    (/anonymous)\n" +
        "  GET /home          [*/*]     [*/*]    (/anonymous)\n" +
        "  WS  /ws            [text/plain]     [text/plain]\n" +
        "\n" +
        "listening on:\n" +
        "  http://localhost:8080/", setup);
  }

  @Test
  public void printConfig() {
    AppPrinter printer = new AppPrinter(
        Sets.newLinkedHashSet(
            Arrays.asList(before("/"), beforeSend("/"), after("/"), route("/"), route("/home"))),
        Sets.newLinkedHashSet(Arrays.asList(socket("/ws"))), config("/"));
    Logger log = (Logger) LoggerFactory.getLogger(AppPrinterTest.class);
    log.setLevel(Level.DEBUG);
    printer.printConf(log, config("/"));
  }

  @Test
  public void printHttps() {
    String setup = new AppPrinter(
        Sets.newLinkedHashSet(Arrays.asList(route("/"), route("/home"))),
        Sets.newLinkedHashSet(Arrays.asList(socket("/ws"))),
        config("/").withValue("application.securePort", ConfigValueFactory.fromAnyRef(8443)))
            .toString();
    assertEquals("  GET /        [*/*]     [*/*]    (/anonymous)\n" +
        "  GET /home    [*/*]     [*/*]    (/anonymous)\n" +
        "  WS  /ws      [text/plain]     [text/plain]\n" +
        "\n" +
        "listening on:\n" +
        "  http://localhost:8080/\n" +
        "  https://localhost:8443/", setup);
  }

  @Test
  public void printHttp2() {
    String setup = new AppPrinter(
        Sets.newLinkedHashSet(Arrays.asList(route("/"), route("/home"))),
        Sets.newLinkedHashSet(Arrays.asList(socket("/ws"))),
        config("/")
            .withValue("server.http2.enabled", ConfigValueFactory.fromAnyRef(true))
            .withValue("application.securePort", ConfigValueFactory.fromAnyRef(8443)))
                .toString();
    assertEquals("  GET /        [*/*]     [*/*]    (/anonymous)\n" +
        "  GET /home    [*/*]     [*/*]    (/anonymous)\n" +
        "  WS  /ws      [text/plain]     [text/plain]\n" +
        "\n" +
        "listening on:\n" +
        "  http://localhost:8080/  +h2\n" +
        "  https://localhost:8443/ +h2", setup);
  }

  @Test
  public void printHttp2Https() {
    String setup = new AppPrinter(
        Sets.newLinkedHashSet(Arrays.asList(route("/"), route("/home"))),
        Sets.newLinkedHashSet(Arrays.asList(socket("/ws"))),
        config("/")
            .withValue("server.http2.cleartext", ConfigValueFactory.fromAnyRef(false))
            .withValue("server.http2.enabled", ConfigValueFactory.fromAnyRef(true))
            .withValue("application.securePort", ConfigValueFactory.fromAnyRef(8443)))
                .toString();
    assertEquals("  GET /        [*/*]     [*/*]    (/anonymous)\n" +
        "  GET /home    [*/*]     [*/*]    (/anonymous)\n" +
        "  WS  /ws      [text/plain]     [text/plain]\n" +
        "\n" +
        "listening on:\n" +
        "  http://localhost:8080/\n" +
        "  https://localhost:8443/ +h2", setup);
  }

  @Test
  public void printHttp2ClearText() {
    String setup = new AppPrinter(
        Sets.newLinkedHashSet(Arrays.asList(route("/"), route("/home"))),
        Sets.newLinkedHashSet(Arrays.asList(socket("/ws"))),
        config("/")
            .withValue("server.http2.cleartext", ConfigValueFactory.fromAnyRef(true))
            .withValue("server.http2.enabled", ConfigValueFactory.fromAnyRef(true)))
                .toString();
    assertEquals("  GET /        [*/*]     [*/*]    (/anonymous)\n" +
        "  GET /home    [*/*]     [*/*]    (/anonymous)\n" +
        "  WS  /ws      [text/plain]     [text/plain]\n" +
        "\n" +
        "listening on:\n" +
        "  http://localhost:8080/  +h2", setup);
  }

  private Config config(final String path) {
    return ConfigFactory.empty()
        .withValue("application.host", ConfigValueFactory.fromAnyRef("localhost"))
        .withValue("application.port", ConfigValueFactory.fromAnyRef("8080"))
        .withValue("server.http2.enabled", ConfigValueFactory.fromAnyRef(false))
        .withValue("server.http2.cleartext", ConfigValueFactory.fromAnyRef(true))
        .withValue("application.path", ConfigValueFactory.fromAnyRef(path));
  }

  @Test
  public void printWithPath() {
    String setup = new AppPrinter(
        Sets.newLinkedHashSet(Arrays.asList(route("/"), route("/home"))),
        Sets.newLinkedHashSet(Arrays.asList(socket("/ws"))), config("/app"))
            .toString();
    assertEquals("  GET /        [*/*]     [*/*]    (/anonymous)\n" +
        "  GET /home    [*/*]     [*/*]    (/anonymous)\n" +
        "  WS  /ws      [text/plain]     [text/plain]\n" +
        "\n" +
        "listening on:\n" +
        "  http://localhost:8080/app", setup);
  }

  @Test
  public void printNoSockets() {
    String setup = new AppPrinter(
        Sets.newLinkedHashSet(Arrays.asList(route("/"), route("/home"))),
        Sets.newLinkedHashSet(), config("/app"))
            .toString();
    assertEquals("  GET /        [*/*]     [*/*]    (/anonymous)\n" +
        "  GET /home    [*/*]     [*/*]    (/anonymous)\n" +
        "\n" +
        "listening on:\n" +
        "  http://localhost:8080/app", setup);
  }

  private Route.Definition route(final String pattern) {
    return new Route.Definition("GET", pattern, (req, rsp) -> {
    });
  }

  private Route.Definition before(final String pattern) {
    return new Route.Definition("GET", pattern, (Before) (req, rsp) -> {
    });
  }

  private Route.Definition beforeSend(final String pattern) {
    return new Route.Definition("GET", pattern, (Route.After) (req, rsp, r) -> {
      return r;
    });
  }

  private Route.Definition after(final String pattern) {
    return new Route.Definition("GET", pattern, (Route.Complete) (req, rsp, r) -> {
    });
  }

  private WebSocket.Definition socket(final String pattern) {
    return new WebSocket.Definition(pattern, (req, ws) -> {
    });
  }
}
