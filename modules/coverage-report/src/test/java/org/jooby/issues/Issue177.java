package org.jooby.issues;

import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue177 extends ServerFeature {

  @Path("/issue177")
  public static class Resource {

    @GET
    public String param(final String x) {
      return x;
    }

  }

  {
    use(Resource.class);

    err((req, rsp, err) -> {
      rsp.send(err.getMessage());
    });
  }

  @Test
  public void shouldSeeParamNameOnMvcRoute() throws Exception {
    request()
        .get("/issue177")
        .expect(400)
        .expect("Bad Request(400): Required parameter 'x' is not present");
  }

}
