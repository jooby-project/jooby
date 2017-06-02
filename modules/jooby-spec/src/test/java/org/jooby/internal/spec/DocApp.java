package org.jooby.internal.spec;

import java.util.List;

import org.jooby.Jooby;
import org.jooby.Results;
import org.jooby.spec.RouteProcessor;
import org.jooby.spec.RouteSpec;
import org.junit.Test;

import apps.DB;
import apps.model.Pet;

public class DocApp extends Jooby {

  {
    /**
     * Everything about your Pets.
     */
    use("/api/pets")
        /**
         * List pets ordered by name.
         *
         * @param start Start offset, useful for paging. Default is <code>0</code>.
         * @param max Max page size, useful for paging. Default is <code>200</code>.
         * @return Pets ordered by name.
         */
        .get(req -> {
          int start = req.param("start").intValue(0);
          int max = req.param("max").intValue(200);
          DB db = req.require(DB.class);
          List<Pet> pets = db.findAll(Pet.class, start, max);
          return pets;
        })
        /**
         * Find pet by ID
         *
         * @param id Pet ID.
         * @return Returns <code>200</code> with a single pet or <code>404</code>
         */
        .get("/:id", req -> {
          int id = req.param("id").intValue();
          DB db = req.require(DB.class);
          Pet pet = db.find(Pet.class, id);
          return pet;
        })
        /**
         * Add a new pet to the store.
         *
         * @param body Pet object that needs to be added to the store.
         * @return Returns a saved pet.
         */
        .post(req -> {
          Pet pet = req.body().to(Pet.class);
          DB db = req.require(DB.class);
          db.save(pet);
          return pet;
        })
        /**
         * Update an existing pet.
         *
         * @param body Pet object that needs to be updated.
         * @return Returns a saved pet.
         */
        .put(req -> {
          Pet pet = req.body().to(Pet.class);
          DB db = req.require(DB.class);
          db.save(pet);
          return pet;
        })
        /**
         * Deletes a pet by ID.
         *
         * @param id Pet ID.
         * @return A <code>204</code>
         */
        .delete("/:id", req -> {
          int id = req.param("id").intValue();
          DB db = req.require(DB.class);
          db.delete(Pet.class, id);
          return Results.noContent();
        })
        .produces("json")
        .consumes("json");
  }

  @Test
  public void check() throws Exception {
    List<RouteSpec> specs1 = new RouteProcessor().process(new DocApp());
    specs1.forEach(System.out::println);
  }
}
