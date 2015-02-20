package org.jooby.netty;

import javax.inject.Singleton;

import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.internal.netty.NettyServer;
import org.jooby.spi.Server;

import com.google.inject.Binder;
import com.typesafe.config.Config;

public class Netty implements Jooby.Module {

  @Override
  public void configure(final Env env, final Config config, final Binder binder) {
    binder.bind(Server.class).to(NettyServer.class).in(Singleton.class);
  }

}
