package org.jooby.issues;

import java.util.Map;

import org.jooby.json.Jackson;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue157 extends ServerFeature {

  {
    use(new Jackson());

    post("/json-and-headers", req -> {
      req.header("Origin").toOptional();
      req.param("X").toOptional();
      return req.body().to(Map.class);
    });
  }

  @Test
  public void postWithHeaders() throws Exception {
    request()
        .post("/json-and-headers?X=x")
        .body("{}", "application/json")
        .header("Origin", "http://localhost")
        .expect("{}");
  }
}
