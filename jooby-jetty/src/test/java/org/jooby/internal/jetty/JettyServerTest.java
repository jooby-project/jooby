package org.jooby.internal.jetty;

import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;

import java.util.Map;

import javax.inject.Provider;

import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.eclipse.jetty.websocket.api.WebSocketBehavior;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.server.WebSocketServerFactory;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.jooby.spi.HttpHandler;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

@RunWith(PowerMockRunner.class)
@PrepareForTest({JettyServer.class, Server.class, QueuedThreadPool.class, ServerConnector.class,
    HttpConfiguration.class, HttpConnectionFactory.class, WebSocketPolicy.class,
    WebSocketServerFactory.class })
public class JettyServerTest {

  Map<String, Object> httpConfig = ImmutableMap.<String, Object> builder()
      .put("HeaderCacheSize", "8k")
      .put("RequestHeaderSize", "8k")
      .put("ResponseHeaderSize", "8k")
      .put("FileSizeThreshold", "16k")
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
          .build())
      .build();

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
      .withValue("jetty.threads.MaxThreads", ConfigValueFactory.fromAnyRef("10"))
      .withValue("jetty.threads.IdleTimeout", ConfigValueFactory.fromAnyRef("3s"))
      .withValue("jetty.threads.Name", ConfigValueFactory.fromAnyRef("jetty task"))
      .withValue("jetty.FileSizeThreshold", ConfigValueFactory.fromAnyRef(1024))
      .withValue("jetty.url.charset", ConfigValueFactory.fromAnyRef("UTF-8"))
      .withValue("jetty.http", ConfigValueFactory.fromAnyRef(httpConfig))
      .withValue("jetty.ws", ConfigValueFactory.fromAnyRef(ws))
      .withValue("server.http.MaxRequestSize", ConfigValueFactory.fromAnyRef("200k"))
      .withValue("server.http2.enabled", ConfigValueFactory.fromAnyRef(false))
      .withValue("application.port", ConfigValueFactory.fromAnyRef(6789))
      .withValue("application.host", ConfigValueFactory.fromAnyRef("0.0.0.0"))
      .withValue("application.tmpdir", ConfigValueFactory.fromAnyRef("target"));

  private MockUnit.Block pool = unit -> {
    QueuedThreadPool pool = unit.mockConstructor(QueuedThreadPool.class);
    unit.registerMock(QueuedThreadPool.class, pool);

    pool.setMaxThreads(10);
    pool.setMinThreads(1);
    pool.setIdleTimeout(3000);
    pool.setName("jetty task");
  };

  private MockUnit.Block server = unit -> {
    Server server = unit.constructor(Server.class)
        .args(ThreadPool.class)
        .build(unit.get(QueuedThreadPool.class));

    server.setStopAtShutdown(false);
    server.setHandler(isA(JettyHandler.class));
    server.start();
    server.join();
    server.stop();

    unit.registerMock(Server.class, server);
  };

  private MockUnit.Block httpConf = unit -> {
    HttpConfiguration conf = unit.mockConstructor(HttpConfiguration.class);
    conf.setOutputBufferSize(32768);
    conf.setRequestHeaderSize(8192);
    conf.setSendXPoweredBy(false);
    conf.setHeaderCacheSize(8192);
    conf.setSendServerVersion(false);
    conf.setSendDateHeader(false);
    conf.setResponseHeaderSize(8192);

    unit.registerMock(HttpConfiguration.class, conf);
  };

  private MockUnit.Block httpFactory = unit -> {
    HttpConnectionFactory factory = unit.constructor(HttpConnectionFactory.class)
        .args(HttpConfiguration.class)
        .build(unit.get(HttpConfiguration.class));

    unit.registerMock(HttpConnectionFactory.class, factory);
  };

  private MockUnit.Block connector = unit -> {
    ServerConnector connector = unit.constructor(ServerConnector.class)
        .args(Server.class, ConnectionFactory[].class)
        .build(unit.get(HttpConnectionFactory.class));

    connector.setSoLingerTime(-1);
    connector.setIdleTimeout(3000);
    connector.setStopTimeout(3000);
    connector.setAcceptQueueSize(0);
    connector.setPort(6789);
    connector.setHost("0.0.0.0");

    unit.registerMock(ServerConnector.class, connector);

    Server server = unit.get(Server.class);
    server.addConnector(connector);
  };

  private Block wsPolicy = unit -> {
    WebSocketPolicy policy = unit.constructor(WebSocketPolicy.class)
        .args(WebSocketBehavior.class)
        .build(WebSocketBehavior.SERVER);

    policy.setAsyncWriteTimeout(60000L);
    policy.setMaxBinaryMessageSize(65536);
    policy.setMaxBinaryMessageBufferSize(32000);
    policy.setIdleTimeout(300000L);
    policy.setMaxTextMessageSize(65536);
    policy.setMaxTextMessageBufferSize(32768);
    policy.setInputBufferSize(4096);

    unit.registerMock(WebSocketPolicy.class, policy);
  };

  private Block wsFactory = unit -> {
    WebSocketServerFactory factory = unit.constructor(WebSocketServerFactory.class)
        .args(WebSocketPolicy.class)
        .build(unit.get(WebSocketPolicy.class));

    factory.setCreator(isA(WebSocketCreator.class));

    factory.setStopTimeout(30000L);

    unit.registerMock(WebSocketServerFactory.class, factory);
  };

  @SuppressWarnings("unchecked")
  @Test
  public void startStopServer() throws Exception {

    new MockUnit(HttpHandler.class, Provider.class)
        .expect(pool)
        .expect(server)
        .expect(httpConf)
        .expect(httpFactory)
        .expect(connector)
        .expect(wsPolicy)
        .expect(wsFactory)
        .run(unit -> {
          JettyServer server = new JettyServer(unit.get(HttpHandler.class), config, unit.get(Provider.class));

          server.start();
          server.join();
          server.stop();
        });
  }

  @SuppressWarnings("unchecked")
  @Test(expected = IllegalArgumentException.class)
  public void badOption() throws Exception {

    new MockUnit(HttpHandler.class, Provider.class)
        .expect(unit -> {
          QueuedThreadPool pool = unit.mockConstructor(QueuedThreadPool.class);
          unit.registerMock(QueuedThreadPool.class, pool);

          pool.setMaxThreads(10);
          expectLastCall().andThrow(new IllegalArgumentException("10"));
        })
        .run(unit -> {
          new JettyServer(unit.get(HttpHandler.class), config, unit.get(Provider.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test(expected = ConfigException.BadValue.class)
  public void badConfOption() throws Exception {

    new MockUnit(HttpHandler.class, Provider.class)
        .run(unit -> {
          new JettyServer(unit.get(HttpHandler.class),
              config.withValue("jetty.threads.MinThreads", ConfigValueFactory.fromAnyRef("x")),
              unit.get(Provider.class));
        });
  }

}
