package org.jooby.internal.undertow;

import org.jooby.MockUnit;
import org.jooby.spi.ApplicationHandler;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class UndertowServerTest {

  @Test
  public void server() throws Exception {
    Config config = ConfigFactory.empty()
        .withValue("undertow.ioThreads", ConfigValueFactory.fromAnyRef("2"))
        .withValue("undertow.workerThreads", ConfigValueFactory.fromAnyRef("1"))
        .withValue("undertow.bufferSize", ConfigValueFactory.fromAnyRef("16k"))
        .withValue("undertow.directBuffers", ConfigValueFactory.fromAnyRef(true))
        .withValue("undertow.buffersPerRegion", ConfigValueFactory.fromAnyRef("1"))
        .withValue("undertow.server.REUSE_ADDRESSES", ConfigValueFactory.fromAnyRef(true))
        .withValue("undertow.server.MAX_ENTITY_SIZE", ConfigValueFactory.fromAnyRef("200k"))
        .withValue("undertow.socket.TCP_NODELAY", ConfigValueFactory.fromAnyRef(true))
        .withValue("undertow.worker.REUSE_ADDRESSES", ConfigValueFactory.fromAnyRef(true))
        .withValue("undertow.server.IGNORE_INVALID", ConfigValueFactory.fromAnyRef("bad option"))
        .withValue("application.port", ConfigValueFactory.fromAnyRef(6789))
        .withValue("application.host", ConfigValueFactory.fromAnyRef("0.0.0.0"));

    new MockUnit(ApplicationHandler.class)
        .run(unit -> {
          UndertowServer server = new UndertowServer(unit.get(ApplicationHandler.class), config);
          try {
            server.start();
          } finally {
            server.stop();
          }
        });
  }
}
