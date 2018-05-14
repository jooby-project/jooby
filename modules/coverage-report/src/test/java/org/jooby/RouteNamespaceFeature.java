package org.jooby;

import java.util.HashMap;
import java.util.Map;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class RouteNamespaceFeature extends ServerFeature {

  {

    Map<String, String> db = new HashMap<>();

    use("/pets")
        .get("/:id", req -> db.get(req.param("id").value()))
        .get(() -> db.values())
        .post("/:id", req -> "" + db.put(req.param("id").value(), req.body().value()))
        .put("/:id", req -> db.put(req.param("id").value(), req.body().value()))
        .delete("/:id", req -> db.remove(req.param("id").value()));
  }

  @Test
  public void rest() throws Exception {
    request()
        .post("/pets/1")
        .body("cat", "text/plain")
        .expect("null");

    request()
        .get("/pets/1")
        .expect("cat");

    request()
        .get("/pets")
        .expect("[cat]");

    request()
        .put("/pets/1")
        .body("cat", "text/plain")
        .expect("cat");

    request()
        .delete("/pets/1")
        .expect("cat");

    request()
        .get("/pets")
        .expect("[]");
  }
}
