package org.jooby.jetty;

import javax.inject.Singleton;

import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.internal.jetty.JettyServer;
import org.jooby.spi.Server;

import com.google.inject.Binder;
import com.typesafe.config.Config;

public class Jetty implements Jooby.Module {

  @Override
  public void configure(final Env env, final Config config, final Binder binder) {
    binder.bind(Server.class).to(JettyServer.class).in(Singleton.class);
  }
}
