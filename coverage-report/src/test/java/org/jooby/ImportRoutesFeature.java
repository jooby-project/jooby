package org.jooby;

import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class ImportRoutesFeature extends ServerFeature {

  public static class Resource {

    @Path("/r")
    @GET
    public String hey(final Request req) {
      return req.path();
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

  public static class C extends Jooby {
    {
      get("/1", req -> req.path());

      get("/2", req -> req.path());

      use(Resource.class);
    }
  }

  public static class D extends Jooby {
    {
      use("/routes")
          .get("/1", req -> req.path())
          .get("/2", req -> req.path());
    }
  }

  {

    use(new A());

    use(new B());

    use("/c", new C());

    use("/d", new D());

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

    request()
        .get("/c/1")
        .expect("/c/1");

    request()
        .get("/c/2")
        .expect("/c/2");

    request()
        .get("/c/r")
        .expect("/c/r");

    request()
        .get("/d/routes/1")
        .expect("/d/routes/1");

    request()
        .get("/d/routes/2")
        .expect("/d/routes/2");
  }
}
