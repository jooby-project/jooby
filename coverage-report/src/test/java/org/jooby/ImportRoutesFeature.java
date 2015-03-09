package org.jooby;

import org.jooby.Jooby;
import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class ImportRoutesFeature extends ServerFeature {

  public static class Resource {

    @Path("/r")
    @GET
    public String hey() {
      return "/r";
    }
  }

  public static class A extends Jooby {
    {

      assets("/assets/**");
      get("/a/1", req -> req.path());

    }
  }

  public static class B extends Jooby {
    {
      get("/b/1", req -> req.path());

      use(Resource.class);
    }
  }

  {

    use(new A());

    use(new B());

    get("/1", req -> req.path());
  }

  @Test
  public void importedRoutes() throws Exception {
    request()
        .get("/assets/file.js")
        .expect("function () {}\n");

    request()
        .get("/a/1")
        .expect("/a/1");

    request()
        .get("/b/1")
        .expect("/b/1");

    request()
        .get("/1")
        .expect("/1");

    request()
        .get("/r")
        .expect("/r");
  }
}
