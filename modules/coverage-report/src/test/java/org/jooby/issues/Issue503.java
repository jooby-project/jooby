package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class Issue503 extends ServerFeature {

  {
    use(ConfigFactory.empty().withValue("application.path", ConfigValueFactory.fromAnyRef("/503")));

    get("/", () -> "503");

    err((req, rsp, x) -> {
      rsp.send(req.path(true));
    });
  }

  @Test
  public void shouldNotSeeOddCharacters() throws Exception {
    request()
        .get("/")
        .expect(404)
        .expect("/");

    request()
        .get("/foo")
        .expect(404)
        .expect("/foo");
  }

}
