/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static com.typesafe.config.ConfigValueFactory.fromAnyRef;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.typesafe.config.ConfigFactory;

public class ServerOptionsTest {

  @Test
  public void shouldParseFromConfig() {
    ServerOptions options =
        ServerOptions.from(
                ConfigFactory.empty()
                    .withValue("server.port", fromAnyRef(9090))
                    .withValue("server.securePort", fromAnyRef(9443))
                    .withValue("server.ioThreads", fromAnyRef(4))
                    .withValue("server.name", fromAnyRef("Test"))
                    .withValue("server.bufferSize", fromAnyRef(1024))
                    .withValue("server.defaultHeaders", fromAnyRef(false))
                    .withValue("server.compressionLevel", fromAnyRef(8))
                    .withValue("server.maxRequestSize", fromAnyRef(2048))
                    .withValue("server.workerThreads", fromAnyRef(32))
                    .withValue("server.host", fromAnyRef("0.0.0.0"))
                    .withValue("server.httpsOnly", fromAnyRef(true))
                    .resolve())
            .get();
    assertEquals(9090, options.getPort());
    assertEquals(9443, options.getSecurePort());
    assertEquals(4, options.getIoThreads());
    assertEquals("Test", options.getServer());
    assertEquals(8, options.getCompressionLevel());
    assertEquals(2048, options.getMaxRequestSize());
    assertEquals(32, options.getWorkerThreads());
    assertEquals("0.0.0.0", options.getHost());
    assertTrue(options.isHttpsOnly());
  }

  @Test
  public void shouldSetCorrectLocalHost() {
    ServerOptions options = new ServerOptions();
    assertEquals("0.0.0.0", options.getHost());
    options.setHost("localhost");
    assertEquals("0.0.0.0", options.getHost());
    options.setHost(null);
    assertEquals("0.0.0.0", options.getHost());
    options.setHost("");
    assertEquals("0.0.0.0", options.getHost());
  }
}
