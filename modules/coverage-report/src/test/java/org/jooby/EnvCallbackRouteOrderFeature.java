package org.jooby;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class EnvCallbackRouteOrderFeature extends ServerFeature {

  {
    StringBuilder buff = new StringBuilder();
    on("dev", () -> {
      get("/", (req, rsp) -> {
        buff.append("pre");
      });
    });

    get("/", (req, rsp) -> {
      buff.append("-");
    });

    on("dev", () -> {
      get("/", (req, rsp) -> {
        rsp.send(buff.append("post"));
      });
    });
  }

  @Test
  public void devCallback() throws Exception {
    request().get("/").expect("pre-post");
  }
}
