package org.jooby.issues;

import org.jooby.Request;
import org.jooby.Route;
import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue349b extends ServerFeature {
  @Path("/mvc")
  public static class Resource {

    @GET
    @Path("/a")
    public Object a(final Request req) {
      Route r = req.route();
      return r.name() + ";" + r.attributes().toString() + ";" + r.produces() + ";" + r.consumes()
          + ";";
    }

  }

  {
    use(Resource.class)
        .attr("foo", "bar")
        .name("x")
        .produces("json")
        .consumes("json")
        .excludes("/something")
        .map(v -> "->" + v);
  }

  @Test
  public void mvcSetup() throws Exception {
    request().get("/mvc/a")
        .expect("->/x;{foo=bar};[application/json];[application/json];");
  }

}
