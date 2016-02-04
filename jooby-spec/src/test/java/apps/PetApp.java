package apps;

import org.jooby.Jooby;

import apps.model.Pet;

public class PetApp extends Jooby {

  {

    use("/api/blogs", new BlogApi());

    use("/api/pets")
        /**
         * Get a pet by ID.
         */
        .get("/:id", req -> {
          int id = req.param("id").intValue();
          DB db = req.require(DB.class);
          Pet result = db.find(id);
          return result;
        });

  }
}
