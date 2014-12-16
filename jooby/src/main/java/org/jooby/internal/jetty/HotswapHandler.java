package org.jooby.internal.jetty;

import java.io.IOException;

import org.jooby.internal.RouteHandler;
import org.jooby.internal.hotswap.Hotswap;

import com.google.inject.Injector;
import com.typesafe.config.Config;

public class HotswapHandler extends JettyHandler {

  private Hotswap hotswap;

  public HotswapHandler(final Injector injector, final Config config) throws IOException {
    super(config);

    hotswap = new Hotswap(injector);
  }

  @Override
  protected RouteHandler routeHandler() {
    return hotswap.handler();
  }

  @Override
  protected void doStart() throws Exception {
    hotswap.start();
    super.doStart();
  }

  @Override
  protected void doStop() throws Exception {
    hotswap.stop();
    super.doStop();
  }
}
