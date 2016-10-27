package org.jooby.issues;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.jooby.Deferred;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue498b extends ServerFeature {

  @Path("/mvc")
  public static class Controller {

    @Path("/498")
    public Deferred deferred() {
      return Deferred.deferred(() -> Thread.currentThread().getName());
    }

    @Path("/498/0")
    public Deferred deferred1() {
      return Deferred.deferred(req -> {
        assertNotNull(req);
        return Thread.currentThread().getName();
      });
    }

  }

  {
    get("/498", deferred("direct", req -> {
      assertNotNull(req);
      return Thread.currentThread().getName();
    }));

    get("/498/0", deferred("direct", () -> {
      return Thread.currentThread().getName();
    }));

    use(Controller.class);

    err((req, rsp, x) -> {
      rsp.send(x.getCause().getMessage());
    });
  }

  @Test
  public void functionalDeferred() throws Exception {
    request()
        .get("/498")
        .expect(v -> {
          assertTrue(v.toLowerCase().contains("task"));
        });

    request()
        .get("/498/0")
        .expect(v -> {
          assertTrue(v.toLowerCase().contains("task"));
        });

    request()
        .get("/mvc/498")
        .expect(v -> {
          assertTrue(v.toLowerCase().contains("task"));
        });

    request()
        .get("/mvc/498/0")
        .expect(v -> {
          assertTrue(v.toLowerCase().contains("task"));
        });
  }

}
