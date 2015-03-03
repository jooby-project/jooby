package org.jooby.internal.jetty;

import java.util.Map;

import org.jooby.MockUnit;
import org.jooby.spi.HttpHandler;
import org.jooby.spi.Server;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class JettyServerTest {

  @Test
  public void server() throws Exception {

    Map<String, Object> httpConfig = ImmutableMap.<String, Object> builder()
        .put("HeaderCacheSize", "8k")
        .put("RequestHeaderSize", "8k")
        .put("ResponseHeaderSize", "8k")
        .put("SendServerVersion", false)
        .put("SendXPoweredBy", false)
        .put("SendDateHeader", false)
        .put("OutputBufferSize", "32k")
        .put("BadOption", "bad")
        .put("connector", ImmutableMap.<String, Object> builder()
            .put("AcceptQueueSize", 0)
            .put("SoLingerTime", -1)
            .put("StopTimeout", "3s")
            .put("IdleTimeout", "3s")
            .build()
        ).build();

    Map<String, Object> ws = ImmutableMap.<String, Object> builder()
        .put("MaxTextMessageSize", "64k")
        .put("MaxTextMessageBufferSize", "32k")
        .put("MaxBinaryMessageSize", "64k")
        .put("MaxBinaryMessageBufferSize", "32kB")
        .put("AsyncWriteTimeout", 60000)
        .put("IdleTimeout", "5minutes")
        .put("InputBufferSize", "4k")
        .build();

    Config config = ConfigFactory.empty()
        .withValue("jetty.threads.MinThreads", ConfigValueFactory.fromAnyRef("1"))
        .withValue("jetty.threads.MaxThreads", ConfigValueFactory.fromAnyRef("6"))
        .withValue("jetty.threads.IdleTimeout", ConfigValueFactory.fromAnyRef("3s"))
        .withValue("jetty.url.charset", ConfigValueFactory.fromAnyRef("UTF-8"))
        .withValue("jetty.http", ConfigValueFactory.fromAnyRef(httpConfig))
        .withValue("jetty.ws", ConfigValueFactory.fromAnyRef(ws))
        .withValue("server.http.MaxRequestSize", ConfigValueFactory.fromAnyRef("200k"))
        .withValue("application.port", ConfigValueFactory.fromAnyRef(6789))
        .withValue("application.host", ConfigValueFactory.fromAnyRef("0.0.0.0"))
        .withValue("application.tmpdir", ConfigValueFactory.fromAnyRef("target"));

    new MockUnit(HttpHandler.class)
        .run(unit -> {
          Server server = new JettyServer(unit.get(HttpHandler.class), config);
          try {
            server.start();
          } finally {
            server.stop();
          }
        });
  }
}
