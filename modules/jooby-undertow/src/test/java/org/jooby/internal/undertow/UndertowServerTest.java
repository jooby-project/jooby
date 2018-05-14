package org.jooby.internal.undertow;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Provider;

import org.jooby.spi.HttpHandler;
import org.jooby.test.MockUnit;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class UndertowServerTest {

  @SuppressWarnings("unchecked")
  @Test
  public void server() throws Exception {
    Config config = ConfigFactory.empty()
        .withValue("undertow.ioThreads", ConfigValueFactory.fromAnyRef("2"))
        .withValue("undertow.workerThreads", ConfigValueFactory.fromAnyRef("1"))
        .withValue("undertow.bufferSize", ConfigValueFactory.fromAnyRef("16k"))
        .withValue("undertow.directBuffers", ConfigValueFactory.fromAnyRef(true))
        .withValue("undertow.buffersPerRegion", ConfigValueFactory.fromAnyRef("1"))
        .withValue("undertow.server.REUSE_ADDRESSES", ConfigValueFactory.fromAnyRef(true))
        .withValue("undertow.server.IDLE_TIMEOUT", ConfigValueFactory.fromAnyRef("60s"))
        .withValue("undertow.server.MAX_ENTITY_SIZE", ConfigValueFactory.fromAnyRef("200k"))
        .withValue("undertow.socket.TCP_NODELAY", ConfigValueFactory.fromAnyRef(true))
        .withValue("undertow.worker.REUSE_ADDRESSES", ConfigValueFactory.fromAnyRef(true))
        .withValue("undertow.server.IGNORE_INVALID", ConfigValueFactory.fromAnyRef("bad option"))
        .withValue("undertow.awaitShutdown", ConfigValueFactory.fromAnyRef(1000))
        .withValue("application.port", ConfigValueFactory.fromAnyRef(6789))
        .withValue("application.host", ConfigValueFactory.fromAnyRef("0.0.0.0"))
        .withValue("server.http2.enabled", ConfigValueFactory.fromAnyRef(false));

    new MockUnit(HttpHandler.class, Provider.class)
        .run(unit -> {
          UndertowServer server = new UndertowServer(unit.get(HttpHandler.class), config,
              unit.get(Provider.class));
          try {
            assertNotNull(server.executor());
            server.start();
            assertTrue(server.executor().isPresent());
            server.join();// NOOP
          } finally {
            server.stop();
          }
        });
  }
}
