/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i4056;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.jooby.Jooby;
import io.jooby.Server;
import io.jooby.ServerOptions;
import io.jooby.jetty.JettyServer;
import io.jooby.junit.ServerProvider;
import io.jooby.netty.NettyServer;
import io.jooby.undertow.UndertowServer;

public class Issue4056 {

  @ParameterizedTest
  @ValueSource(classes = {JettyServer.class, NettyServer.class, UndertowServer.class})
  public void serverStartedByHandHasItsOptions(Class<?> serverClass) throws Exception {
    var app = new Jooby();
    app.get("/4056", ctx -> ctx.getServerPort());

    Server server = new ServerProvider(serverClass).get(new ServerOptions().setPort(0));
    server.start(app);
    int port = server.getOptions().getPort();
    try (var client = HttpClient.newHttpClient()) {
      var rsp =
          client.send(
              HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/4056")).build(),
              HttpResponse.BodyHandlers.ofString());
      assertEquals(200, rsp.statusCode());
      assertEquals(String.valueOf(port), rsp.body());
    } finally {
      server.stop();
    }
  }
}
