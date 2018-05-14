package apps;

import java.util.List;

import org.jooby.Jooby;

public class FilterApp extends Jooby {

  {

    /**
     * Home page.
     */
    get("/", (req, rsp, chain) -> {
      rsp.send("Hi");
      chain.next(req, rsp);
    });

    /**
     * API pets.
     */
    use("/api/pets")
        // Get a Pet by ID.
        .get("/:id", (req, rsp) -> {
          int id = req.param("id").intValue();
          DB db = req.require(DB.class);
          LocalType result = db.find(id);
          rsp.send(result);
        }).get((req, rsp) -> {
          int start = req.param("start").intValue(0);
          int max = req.param("max").intValue(200);

          DB db = req.require(DB.class);
          List<LocalType> results = db.findAll(start, max);

          rsp.send(results);
        }).post((req, rsp) -> {
          LocalType body = req.body().to(LocalType.class);

          DB db = req.require(DB.class);
          body = db.create(body);

          rsp.send(body);
        }).delete("/:id", (req, rsp) -> {
          int id = req.param("id").intValue();
          DB db = req.require(DB.class);
          LocalType result = db.delete(id);
          rsp.status(204).send(result);
        });
  }
}
