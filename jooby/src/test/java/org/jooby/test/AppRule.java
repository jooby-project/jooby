package org.jooby.test;

import static java.util.Objects.requireNonNull;

import org.jooby.Env;
import org.jooby.Jooby;
import org.junit.rules.ExternalResource;

import com.google.inject.Binder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class AppRule extends ExternalResource {

  private static class NoJoin implements Jooby.Module {

    @Override
    public void configure(final Env env, final Config config, final Binder binder) {
    }

    @Override
    public Config config() {
       return ConfigFactory.empty("test-config")
       .withValue("server.join", ConfigValueFactory.fromAnyRef(false));
    }
  }

  private Jooby app;

  public AppRule(final Jooby app) {
    this.app = requireNonNull(app, "App is required.");

    app.use(new NoJoin());
  }

  @Override
  protected void before() throws Throwable {
    app.start();
  }

  @Override
  protected void after() {
    app.stop();
  }
}
