package org.jooby.issues;

import java.util.Map;

import org.jooby.json.Jackson;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue155 extends ServerFeature {

  {

    use(new Jackson());

    post("/i155", req -> {
      req.param("opt").toOptional();
      return req.body().to(Map.class);
    });

    post("/i155/body", req -> {
      return req.body().toOptional(Map.class);
    });

  }

  @Test
  public void shouldIgnoreOptionalParam() throws Exception {
    request()
        .post("/i155")
        .body("{\"name\":\"cat\"}", "application/json")
        .expect(200);
  }

  @Test
  public void shouldIgnoreMissingBody() throws Exception {
    request()
        .post("/i155/body")
        .header("Content-Type", "application/json")
        .expect("null");

    request()
        .post("/i155/body")
        .body("{\"name\":\"cat\"}", "application/json")
        .expect("{\"name\":\"cat\"}");
  }

}
