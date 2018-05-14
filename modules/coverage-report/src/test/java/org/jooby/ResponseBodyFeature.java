package org.jooby;

import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class ResponseBodyFeature extends ServerFeature {

  public static class Resource {

    @GET
    @Path("/200")
    public Result ok() {
      return Results.ok();
    }

    @GET
    @Path("/200/body")
    public Result okWithBody() {
      return Results.ok("***");
    }

    @GET
    @Path("/204")
    public Result noContent() {
      return Results.noContent();
    }

    @GET
    @Path("/headers")
    public Result headers() {
      return Results.ok().header("x", "y");
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
