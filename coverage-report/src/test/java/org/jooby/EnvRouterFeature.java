package org.jooby;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class EnvRouterFeature extends ServerFeature {

  {
    use((env, conf, binder) -> {
      env.routes().get("/", () -> "router");
    });

  }

  @Test
  public void envRouter() throws Exception {
    request().get("/").expect("router");
  }
}
