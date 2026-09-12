/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import io.jooby.output.OutputFactory;

class Issue3995 {

  private static class TestServer extends Server.Base {
    @Override
    public String getName() {
      return "test-server";
    }

    @Override
    public OutputFactory getOutputFactory() {
      return OutputFactory.create();
    }

    @Override
    public Server start(Jooby... application) {
      return this;
    }

    @Override
    public Server stop() {
      return this;
    }
  }

  @Test
  void serverInitMustRunAfterFinalServerOptionsAreSet() {
    var server = new TestServer();
    var configured = new ServerOptions().setPort(3995);

    Jooby app = Jooby.createApp(server, ExecutionMode.DEFAULT, () -> new Jooby() {});

    server.setOptions(configured);
    server.init(app);

    var registryOptions = app.getServices().require(ServerOptions.class);
    assertSame(configured, registryOptions);
    assertEquals(3995, registryOptions.getPort());
    assertEquals("test-server", registryOptions.getServer());
  }
}
