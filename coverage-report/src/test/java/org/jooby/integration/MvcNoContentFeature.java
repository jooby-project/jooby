package org.jooby.integration;

import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class MvcNoContentFeature extends ServerFeature {

  public static class Resource {
    @GET
    @Path("/")
    public void noContent() {

    }
  }

  {
    use(Resource.class);
  }

  @Test
  public void noContent() throws Exception {
    request()
        .get("/")
        .expect(204)
        .empty();
  }

}
