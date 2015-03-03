package org.jooby.internal.netty;

import org.jooby.MockUnit;
import org.jooby.spi.HttpHandler;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class NettyServerTest {

  @Test
  public void server() throws Exception {
    Config config = ConfigFactory.empty()
        .withValue("netty.bossThreads", ConfigValueFactory.fromAnyRef(1))
        .withValue("netty.workerThreads", ConfigValueFactory.fromAnyRef(2))
        .withValue("server.threads.Max", ConfigValueFactory.fromAnyRef(2))
        .withValue("netty.options.SO_BACKLOG", ConfigValueFactory.fromAnyRef(1024))
        .withValue("netty.child.options.SO_REUSEADDR", ConfigValueFactory.fromAnyRef(true))
        .withValue("netty.http.MaxContentLength", ConfigValueFactory.fromAnyRef("200k"))
        .withValue("netty.http.MaxInitialLineLength", ConfigValueFactory.fromAnyRef("4k"))
        .withValue("netty.http.MaxHeaderSize", ConfigValueFactory.fromAnyRef("8k"))
        .withValue("netty.http.MaxChunkSize", ConfigValueFactory.fromAnyRef("8k"))
        .withValue("netty.channel.CONNECT_TIMEOUT_MILLIS", ConfigValueFactory.fromAnyRef("1s"))
        .withValue("application.port", ConfigValueFactory.fromAnyRef(6789))
        .withValue("application.host", ConfigValueFactory.fromAnyRef("0.0.0.0"));

    new MockUnit(HttpHandler.class)
        .run(unit -> {
          NettyServer server = new NettyServer(unit.get(HttpHandler.class), config);
          try {
            server.start();
          } finally {
            server.stop();
          }
        });
  }
}
