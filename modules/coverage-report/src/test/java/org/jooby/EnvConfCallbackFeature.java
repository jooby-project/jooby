package org.jooby;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.inject.Key;
import com.google.inject.name.Names;

public class EnvConfCallbackFeature extends ServerFeature {

  {
    Key<String> key = Key.get(String.class, Names.named(("envcallback")));
    on("dev", conf -> {
      use((env, config, binder) -> {
        binder.bind(key).toInstance(env.name());
      });
    });

    get("/", req -> req.require(key));
  }

  @Test
  public void devCallback() throws Exception {
    request().get("/").expect("dev");
  }
}
