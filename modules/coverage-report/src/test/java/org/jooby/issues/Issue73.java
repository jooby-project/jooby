package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.Config;

public class Issue73 extends ServerFeature {

  {

    get("/issue73", req -> req.require(Config.class).getString("missing"));

    err((req, rsp, err) -> {
      rsp.send(err.getCause().getClass().getName());
    });
  }

  @Test
  public void shouldWorkOnExceptionDeclaredAsInnerClass() throws Exception {
    request()
        .get("/issue73")
        .expect("com.typesafe.config.ConfigException$Missing");
  }
}
