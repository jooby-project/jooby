package org.jooby.issues;

import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue349 extends ServerFeature {
  @Path("/mvc")
  public static class Resource {

    @GET
    @Path("/a")
    public Object a() {
      return "a";
    }

    @GET
    @Path("/void")
    public void ignored() {
    }
  }

  {
    get("/a", () -> "a");

    with(() -> {

      get("/b", () -> "b");

      get("/c", req -> "c");

      use(Resource.class);
    }).map(v -> "//" + v);

    get("/d", () -> "d");

    path("/g", () -> {
      get("/a", () -> "a");
    }).map(v -> "//" + v);

    with(() -> {

      get("/double", () -> 2);

      get("/str", req -> "str");
    }).map((final Integer v) -> v * 2);

  }

  @Test
  public void mapper() throws Exception {
    request().get("/a")
        .expect("a");

    request().get("/b")
        .expect("//b");

    request().get("/c")
        .expect("//c");

    request().get("/d")
        .expect("d");

    request().get("/g/a")
        .expect("//a");

    request().get("/mvc/a")
        .expect("//a");

    request().get("/mvc/void")
        .expect(204);
  }

  @Test
  public void applyIntMapper() throws Exception {
    request().get("/double")
        .expect("4");

    request().get("/str")
        .expect("str");
  }
}
