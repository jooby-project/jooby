package org.jooby;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class EnvCallbackReloadConfFeature extends ServerFeature {

  {
    Key<String> key = Key.get(String.class, Names.named(("envvar")));
    on("dev", () -> {
      use(new Jooby.Module() {

        @Override
        public void configure(final Env env, final Config conf, final Binder binder) {
          binder.bind(key).toInstance(conf.getString("envvar"));
        }

        @Override
        public Config config() {
          return ConfigFactory.empty().withValue("envvar", ConfigValueFactory.fromAnyRef("foo"));
        }
      });
    });

    get("/", req -> req.require(key));
  }

  @Test
  public void devCallback() throws Exception {
    request().get("/").expect("foo");
  }
}
