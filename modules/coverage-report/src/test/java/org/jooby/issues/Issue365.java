package org.jooby.issues;

import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue365 extends ServerFeature {

  public static class Base {

    @GET
    public String list() {
      return "base.list";
    }

    @GET
    @Path("/:id")
    public String findById(final String id) {
      return "base.findById";
    }

  }

  @Path("/api/users")
  public static class Users extends Base {

    @Override
    @GET
    public String list() {
      return "users.list";
    }

    @GET
    @Path("/q/:q")
    public String query(final String q) {
      return "users.query";
    }

  }

  {
    use(Users.class);
  }

  @Test
  public void list() throws Exception {
    request()
        .get("/api/users/1")
        .expect("base.findById");

    request()
        .get("/api/users")
        .expect("users.list");

    request()
        .get("/api/users/q/q")
        .expect("users.query");
  }

}
