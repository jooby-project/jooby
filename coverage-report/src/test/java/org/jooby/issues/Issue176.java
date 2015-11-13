package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue176 extends ServerFeature {

  {
    get("/m/**", req -> req.path());
  }

  @Test
  public void shouldNotMatchRequestStartingWithM() throws Exception {
    request()
        .get("/mobile")
        .expect(404);

    request()
        .get("/mobile/x")
        .expect(404);

    request()
        .get("/movie")
        .expect(404);

    request()
        .get("/movie/x")
        .expect(404);
  }

  @Test
  public void shouldMatchRequestStartingWithM() throws Exception {
    request()
        .get("/m")
        .expect(200);

    request()
        .get("/m/x")
        .expect(200);

    request()
        .get("/m/x/y")
        .expect(200);
  }

}
