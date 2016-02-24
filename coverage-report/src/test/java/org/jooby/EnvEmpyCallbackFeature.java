package org.jooby;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class EnvEmpyCallbackFeature extends ServerFeature {

  {
    on("dev", () -> {
    });

    get("/", () -> "empty");

    on("dev", () -> {
    });
  }

  @Test
  public void devCallback() throws Exception {
    request().get("/").expect("empty");
  }
}
