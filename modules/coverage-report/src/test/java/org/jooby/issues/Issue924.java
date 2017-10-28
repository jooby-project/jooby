package org.jooby.issues;

import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue924 extends ServerFeature {

  @Path("/924")
  public static class Filter {

    @GET
    public void filter(Request req, Response rsp, Route.Chain chain) throws Throwable {
      chain.next(req, rsp);
    }
  }

  {
    use(Filter.class);
    get("/924", () -> "next");
  }

  @Test
  public void chainFromMvc() throws Exception {
    request().get("/924")
        .expect("next")
    .expect(200);
  }

}
