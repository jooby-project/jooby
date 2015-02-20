package org.jooby.internal.jetty;

import javax.inject.Inject;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.websocket.api.WebSocketBehavior;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.server.WebSocketServerFactory;
import org.jooby.spi.Dispatcher;

import com.typesafe.config.Config;

public class JettyServer implements org.jooby.spi.Server {

  private Server server;

  @Inject
  public JettyServer(final Dispatcher dispatcher, final Config config) {
    this.server = server(dispatcher, config);
  }

  private Server server(final Dispatcher dispatcher, final Config config) {
    Server server = new Server();
    server.setStopAtShutdown(false);

    // HTTP connector
    ServerConnector http = new ServerConnector(server);
    http.setPort(config.getInt("application.port"));

    server.addConnector(http);

    WebSocketServerFactory webSocketServerFactory = new WebSocketServerFactory(
        new WebSocketPolicy(WebSocketBehavior.SERVER)
        );
    webSocketServerFactory.setCreator((req, rsp) -> {
      JettyWebSocket ws = new JettyWebSocket();
      req.getHttpServletRequest().setAttribute(JettyWebSocket.class.getName(), ws);
      return ws;
    });

    JettyHandler handler = new JettyHandler(dispatcher, webSocketServerFactory,
        config.getString("application.tmpdir")
        );

    server.setHandler(handler);

    return server;
  }

  @Override
  public void start() throws Exception {
    server.start();
  }

  @Override
  public void join() throws InterruptedException {
    server.join();
  }

  @Override
  public void stop() throws Exception {
    server.stop();
  }

}
