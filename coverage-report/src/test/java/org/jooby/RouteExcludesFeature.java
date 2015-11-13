package org.jooby;

import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class RouteExcludesFeature extends ServerFeature {

  @Path(value = "/parent/**", excludes = "/parent/logout")
  public static class ResourceWithParentExcludes {

    @GET
    public String handle(final Request req) {
      return req.path();
    }
  }

  public static class ResourceWithMethodExcludes {

    @GET
    @Path(value = "/m/**", excludes = "/m/logout")
    public String handle(final Request req) {
      return req.path();
    }
  }

  @Path(value = "/merge/**", excludes = "/merge/logout")
  public static class ResourceWithMergeExcludes {

    @GET
    @Path(value = "/", excludes = "/merge/login")
    public String handle(final Request req) {
      return req.path();
    }
  }

  {

    use(ResourceWithParentExcludes.class);

    use(ResourceWithMethodExcludes.class);

    use(ResourceWithMergeExcludes.class);

    use("/path/**", req -> req.path())
        .excludes("/path/logout");

  }

  @Test
  public void excludes() throws Exception {
    request()
        .get("/path")
        .expect("/path");

    request()
        .get("/path/x")
        .expect("/path/x");

    request()
        .get("/logout")
        .expect(404);
  }

  @Test
  public void parentMvc() throws Exception {
    request()
        .get("/parent")
        .expect("/parent");

    request()
        .get("/parent/x")
        .expect("/parent/x");

    request()
        .get("/parent/logout")
        .expect(404);
  }

  @Test
  public void methodMvc() throws Exception {
    request()
        .get("/m")
        .expect("/m");

    request()
        .get("/m/x")
        .expect("/m/x");

    request()
        .get("/m/logout")
        .expect(404);
  }

  @Test
  public void mergeMvc() throws Exception {
    request()
        .get("/merge")
        .expect("/merge");

    request()
        .get("/merge/x")
        .expect("/merge/x");

    request()
        .get("/merge/logout")
        .expect(404);

    request()
        .get("/merge/login")
        .expect(404);
  }

}
