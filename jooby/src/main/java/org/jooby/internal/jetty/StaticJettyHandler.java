package org.jooby.internal.jetty;

import org.jooby.internal.RouteHandler;

import com.google.inject.Injector;
import com.typesafe.config.Config;

public class StaticJettyHandler extends JettyHandler {

  private final RouteHandler handler;

  public StaticJettyHandler(final Injector injector, final Config config) {
    super(config);
    handler = injector.getInstance(RouteHandler.class);
  }

  @Override
  protected RouteHandler routeHandler() {
    return handler;
  }

}
