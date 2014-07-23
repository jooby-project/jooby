package jooby.jetty;

import javax.inject.Inject;
import javax.inject.Singleton;

import jooby.Mode;
import jooby.internal.RouteHandler;
import jooby.internal.jetty.JettyServerBuilder;
import jooby.jetty.Jetty;

import org.eclipse.jetty.server.Server;

import com.google.inject.Binder;
import com.typesafe.config.Config;

public class JettyTest extends Jetty {

  private static class TestServer implements jooby.Server {

    private Server server;

    @Inject
    public TestServer(final Config config,
        final RouteHandler routeHandler) throws Exception {
      server = JettyServerBuilder.build(config, routeHandler);
    }

    @Override
    public void start() throws Exception {
      server.start();

    }

    @Override
    public void stop() throws Exception {
      server.stop();
    }

  }

  @Override
  public void configure(final Mode mode, final Config config, final Binder binder) {
    binder.bind(jooby.Server.class).to(TestServer.class).in(Singleton.class);
  }

}
