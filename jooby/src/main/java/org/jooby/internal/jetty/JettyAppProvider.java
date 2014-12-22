package org.jooby.internal.jetty;

import org.eclipse.jetty.deploy.App;
import org.eclipse.jetty.deploy.providers.ScanningAppProvider;
import org.eclipse.jetty.server.handler.ContextHandler;

import com.typesafe.config.Config;

public class JettyAppProvider extends ScanningAppProvider {

  public JettyAppProvider(final String contextPath, final Config config) {
    // TODO Auto-generated constructor stub
  }

  @Override
  public ContextHandler createContextHandler(final App app) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

}
