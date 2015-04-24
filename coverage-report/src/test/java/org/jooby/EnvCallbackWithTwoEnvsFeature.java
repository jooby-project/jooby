package org.jooby;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.inject.Key;
import com.google.inject.name.Names;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class EnvCallbackWithTwoEnvsFeature extends ServerFeature {

  {
    use(ConfigFactory.empty().withValue("application.env", ConfigValueFactory.fromAnyRef("stage")));

    Key<String> key = Key.get(String.class, Names.named(("envcallback")));
    on("dev", "stage", () -> {
      use((env, conf, binder) -> {
        binder.bind(key).toInstance(env.name());
      });
    });

    get("/", req -> req.require(key));
  }

  @Test
  public void devCallback() throws Exception {
    request().get("/").expect("stage");
  }
}
