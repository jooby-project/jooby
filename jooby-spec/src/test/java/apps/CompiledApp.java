package apps;

import java.util.List;

import org.jooby.Jooby;

import apps.model.Pet;

public class CompiledApp extends Jooby {
  {
    /**
     * Home page.
     */
    get("/", () -> "Hi");

    /**
     * API pets.
     */
    use("/api/pets")
        // Get a Pet by ID.
        .get("/:id", req -> {
          int id = req.param("id").intValue();
          DB db = req.require(DB.class);
          Pet result = db.find(id);
          return result;
        }).get(req -> {
          int start = req.param("start").intValue(0);
          int max = req.param("max").intValue(200);

          DB db = req.require(DB.class);
          List<Pet> results = db.findAll(start, max);

          return results;
        }).post(req -> {
          Pet body = req.body().to(Pet.class);

          DB db = req.require(DB.class);
          body = db.create(body);

          return body;
        }).delete("/:id", req -> {
          int id = req.param("id").intValue();
          DB db = req.require(DB.class);
          Pet result = db.delete(id);
          return result;
        });

  }
}
