package apps;

import org.jooby.Jooby;
import org.jooby.Results;

import apps.model.Pet;

public class AppOverride extends Jooby {

  {

    /**
     * Delete something by ID.
     *
     * @return It deletes something by ID and returns <code>204</code>
     */
    delete("/:id", req -> {
      return Results.noContent();
    });

    /**
     * Get something by ID
     *
     * @return Get a {@link Pet} by ID with a <code>201</code> response code.
     */
    post("/", req -> {
      DB db = req.require(DB.class);
      Pet pet = db.find(0);
      return Results.with(pet, 201);
    });

  }

}
