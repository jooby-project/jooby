package apps;

import org.jooby.Err;
import org.jooby.Jooby;
import org.jooby.Status;
import org.jooby.apitool.ApiTool;

import java.util.List;

public class AppWithDoc extends Jooby {

  private static final int START = 0;

  private static final int MAX = 200;

  {

    /**
     * Home page.
     */
    get("/", () -> "Hi");

    /**
     * Everything about your Pets.
     */
    use("/api/pets")
        /**
         *
         * Find pet by ID.
         *
         * @param id Pet ID.
         * @return Returns <code>200</code> with a single pet or <code>404</code>
         */
        .get("/:id", req -> {
          int id = req.param("id").intValue();
          DB db = req.require(DB.class);
          Pet result = db.find(id);
          if (result == null) {
            throw new Err(Status.NOT_FOUND);
          }
          return result;
        })
        /**
         *
         * List pets ordered by id.
         *
         * @param start Start offset, useful for paging. Default is <code>0</code>.
         * @param max Max page size, useful for paging. Default is <code>50</code>.
         * @return Pets ordered by name.
         */
        .get(req -> {
          int start = req.param("start").intValue(START);
          int max = req.param("max").intValue(MAX);

          DB db = req.require(DB.class);
          List<Pet> results = db.findAll(start, max);

          return results;
        })
        /**
         *
         * Add a new pet to the store.
         *
         * @param body Pet object that needs to be added to the store.
         * @return Returns a saved pet.
         */
        .post(req -> {
          Pet body = req.body().to(Pet.class);

          DB db = req.require(DB.class);
          body = db.create(body);

          return body;
        })
        /**
         *
         * Deletes a pet by ID.
         *
         * @param id Pet ID.
         * @return A <code>204</code>
         */
        .delete("/:id", req -> {
          int id = req.param("id").intValue();
          DB db = req.require(DB.class);
          Pet result = db.delete(id);
          return result;
        });

    use(new ApiTool());
  }

  public static void main(String[] args) {
    run(AppWithDoc::new, args);
  }
}
