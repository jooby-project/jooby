package apps;

import org.jooby.Err;
import org.jooby.Jooby;
import org.jooby.Status;
import org.jooby.apitool.ApiTool;

import java.util.ArrayList;
import java.util.List;

public class ApiDemo extends Jooby {

  private static final int START = 0;

  private static final int MAX = 200;

  {

    /**
     * Everything about your Pets.
     */
    use("/api/pet")
        /**
         *
         * Add a new pet to the store.
         *
         * @param body Pet object that needs to be added to the store.
         * @return Returns a saved pet or <code>405: Invalid input</code>.
         */
        .post(req -> {
          Pet body = req.body(Pet.class);

          DB db = req.require(DB.class);
          body = db.create(body);

          return body;
        })
        /**
         *
         * Update an existing pet.
         *
         * @param body Pet object that needs to be added to the store.
         * @return Returns a saved pet or {400: Invalid ID input}, {404: Pet not found} <code>405: Validation exception</code>.
         */
        .put(req -> {
          Pet body = req.body(Pet.class);

          DB db = req.require(DB.class);
          body = db.create(body);

          return body;
        })
        /**
         * Finds Pets by status.
         * Multiple status values can be provided with comma separated strings
         *
         * @param status Status values that need to be considered for filter.
         * @return Returns <code>200</code> with a single pet or <code>400</code>
         */
        .get("/findByStatus", req -> {
          List<String> status = req.param("status").toList();
          DB db = req.require(DB.class);
          List<Pet> result = new ArrayList<>();
          return result;
        })
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

    /**
     * Access to Petstore orders.
     */
    use("/api/store")
        /**
         * Returns pet inventories by status
         */
        .get("/inventory", req -> {
          return new Inventory();
        });

    use(new ApiTool()
        .modify(r -> r.pattern().startsWith("/api"), it-> {
          System.out.println(it);
        })
        .raml("/raml")
        .swagger("/swagger"));
  }

  public static void main(String[] args) throws Exception {
    run(ApiDemo::new, args);
  }
}
