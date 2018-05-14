package org.jooby.issues;

import org.jooby.Jooby;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue878 extends ServerFeature {

  static class Foo extends Jooby {
    {
      get("/1", () -> "foo");
    }
  }

  static class Bar extends Jooby {
    {
      get("/1", () -> "bar");
    }
  }

  {
    path("/app", () -> {
      use("/foo", new Foo());
      use("/bar", new Bar());
    });

    path("/api/0", () -> {
      get(() -> "/");
      get("/:id", () -> "/id");
      post(() -> "/post");
      put(() -> "/put");
      delete(() -> "/delete");
    });

    path("/api/1", () -> {
      get(req -> "/");
      post(req -> "/post");
      put(req -> "/put");
      delete(req -> "/delete");
    });

    path("/api/2", () -> {
      get((req, rsp) -> rsp.send("/"));
      post((req, rsp) -> rsp.send("/post"));
      put((req, rsp) -> rsp.send("/put"));
      delete((req, rsp) -> rsp.send("/delete"));
    });
  }

  @Test
  public void groupAppUnderPath() throws Exception {
    request().get("/app/foo/1")
        .expect("foo");
    request().get("/app/bar/1")
        .expect("bar");
  }

  @Test
  public void groupRoutesUnderPath() throws Exception {
    request().get("/api/0")
        .expect("/");
    request().get("/api/0/1")
        .expect("/id");
    request().post("/api/0")
        .expect("/post");
    request().put("/api/0")
        .expect("/put");
    request().delete("/api/0")
        .expect("/delete");

    request().get("/api/1")
        .expect("/");
    request().post("/api/1")
        .expect("/post");
    request().put("/api/1")
        .expect("/put");
    request().delete("/api/1")
        .expect("/delete");

    request().get("/api/2")
        .expect("/");
    request().post("/api/2")
        .expect("/post");
    request().put("/api/2")
        .expect("/put");
    request().delete("/api/2")
        .expect("/delete");
  }
}
