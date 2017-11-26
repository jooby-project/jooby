package org.jooby.issues;

import org.jooby.Results;
import org.jooby.ftl.Ftl;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue838 extends ServerFeature {

  {
    use(new Ftl());

    before((req, rsp) -> {
      req.set("session", req.session());
    });

    get("/838", req -> Results.html("org/jooby/ftl/838"));
  }

  @Test
  public void shouldIgnoreDestroyedSession() throws Exception {
    request()
        .get("/838")
        .expect("");
  }
}
