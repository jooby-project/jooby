package org.jooby;

import org.jooby.mvc.GET;
import org.jooby.mvc.POST;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class MvcMethodWithMultipleVerbsFeature extends ServerFeature {

  public static class Resource {
    @GET
    @POST
    @Path("/")
    public String getOrPost(final org.jooby.Request req) {
      return req.route().method().toString();
    }
  }

  {
    use(Resource.class);
  }

  @Test
  public void get() throws Exception {
    request()
        .get("/")
        .expect("GET")
        .expect(200);
  }

  @Test
  public void post() throws Exception {
    request()
        .post("/")
        .expect("POST")
        .expect(200);

  }

}
