package apps;

import org.jooby.Err;
import org.jooby.Jooby;
import org.jooby.Status;

import java.util.List;

public class AppWithDoc extends Jooby {

  private static final int START = 0;

  private static final int MAX = 200;

  {

    {

      /**
       * Home page.
       */
      get("/", () -> "Hi");

      /**
       * Api summary.
       */
      use("/api/pets")
          /**
           * Get a pet by ID.
           *
           * @param id A pet ID.
           * @return A {@link LocalType} with a <code>200 = Success</code> code or <code>404</code>.
           */
          .get("/:id", req -> {
            int id = req.param("id").intValue();
            DB db = req.require(DB.class);
            LocalType result = db.find(id);
            if (result == null) {
              throw new Err(Status.NOT_FOUND);
            }
            return result;
          })
          /**
           * List all pets.
           *
           * @param start Start offset. Optional
           * @param max Max number of results. Optional
           * @return List of pets.
           */
          .get(req -> {
            int start = req.param("start").intValue(START);
            int max = req.param("max").intValue(MAX);

            DB db = req.require(DB.class);
            List<LocalType> results = db.findAll(start, max);

            return results;
          }).post(req -> {
        LocalType body = req.body().to(LocalType.class);

        DB db = req.require(DB.class);
        body = db.create(body);

        return body;
      }).delete("/:id", req -> {
        int id = req.param("id").intValue();
        DB db = req.require(DB.class);
        LocalType result = db.delete(id);
        return result;
      });
    }
  }
}
