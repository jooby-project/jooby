package org.jooby.issues;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.jooby.Route;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue786 extends ServerFeature {

  {
    use("*", (req, rsp, chain) -> {
      List<Route> routes = chain.routes();
      assertEquals(2, routes.size());
      assertEquals("/r2", routes.get(0).name());
      assertEquals("/r3", routes.get(1).name());
      assertEquals("/786/:id", routes.get(routes.size() - 1).pattern());
      chain.next(req, rsp);
    }).name("r1");

    use("/786/**", (req, rsp, chain) -> {
      List<Route> routes = chain.routes();
      assertEquals(1, routes.size());
      assertEquals("/r3", routes.get(0).name());
      assertEquals("/786/:id", routes.get(routes.size() - 1).pattern());
      chain.next(req, rsp);
    }).name("r2");

    get("/786/:id", req -> {
      return req.param("id").value();
    }).name("r3");
  }

  @Test
  public void shouldHaveAccessToRoutePipeline() throws Exception {
    request()
        .get("/786/123")
        .expect("123");
  }

}
