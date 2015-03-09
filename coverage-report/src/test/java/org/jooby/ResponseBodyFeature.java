package org.jooby;

import org.jooby.Body;
import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class ResponseBodyFeature extends ServerFeature {

  public static class Resource {

    @GET
    @Path("/200")
    public Body ok() {
      return Body.ok();
    }

    @GET
    @Path("/200/body")
    public Body okWithBody() {
      return Body.ok("***");
    }

    @GET
    @Path("/204")
    public Body noContent() {
      return Body.noContent();
    }

    @GET
    @Path("/headers")
    public Body headers() {
      return Body.ok().header("x", "y");
    }

  }

  {
    use(Resource.class);
  }

  @Test
  public void ok() throws Exception {
    request()
        .get("/200")
        .expect(200)
        .empty();

    request()
        .get("/200/body")
        .expect("***");
  }

  @Test
  public void notContent() throws Exception {
    request()
        .get("/204")
        .expect(204)
        .empty();
  }

  @Test
  public void headers() throws Exception {
    request()
        .get("/headers")
        .expect(200)
        .header("x", "y")
        .empty();

  }

}
