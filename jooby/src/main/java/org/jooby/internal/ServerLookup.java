package org.jooby.internal;

import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.Jooby.Module;
import org.jooby.spi.Server;

import com.google.inject.Binder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class ServerLookup implements Module {

  private Jooby.Module delegate = null;

  @Override
  public void configure(final Env env, final Config config, final Binder binder) {
    if (config.hasPath("jooby.server")) {
      try {
        delegate = (Jooby.Module) Class.forName(config.getString("jooby.server")).newInstance();
        delegate.configure(env, config, binder);
      } catch (Exception ex) {
        throw new IllegalStateException("No " + Server.class.getName()
            + " implementation was found.", ex);
      }
    }
  }

  @Override
  public void start() {
    if (delegate != null) {
      delegate.start();
    }
  }

  @Override
  public void stop() {
    if (delegate != null) {
      delegate.stop();
    }
  }

  @Override
  public Config config() {
    return ConfigFactory.parseResources(Server.class, "server.conf");
  }

}
