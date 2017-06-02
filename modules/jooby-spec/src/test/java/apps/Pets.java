package apps;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.jooby.mvc.Body;
import org.jooby.mvc.Consumes;
import org.jooby.mvc.GET;
import org.jooby.mvc.POST;
import org.jooby.mvc.PUT;
import org.jooby.mvc.Path;
import org.jooby.mvc.Produces;

import apps.model.Pet;

/**
 * Everything about your Pets.
 */
@Path("/api/pets")
@Consumes("json")
@Produces("json")
public class Pets {

  private DB db;

  @Inject
  public Pets(final DB db) {
    this.db = db;
  }

  /**
   * List pets ordered by name.
   *
   * @param start Start offset, useful for paging. Default is <code>0</code>.
   * @param max Max page size, useful for paging. Default is <code>200</code>.
   * @return Pets ordered by name.
   */
  @GET
  public List<Pet> list(final Optional<Integer> start, final Optional<Integer> max) {
    List<Pet> pets = db.findAll(Pet.class, start.orElse(0), max.orElse(200));
    return pets;
  }

  /**
   * Find pet by ID.
   *
   * @param id Pet ID.
   * @return Returns a single pet
   */
  @Path("/:id")
  @GET
  public Pet get(final int id) {
    Pet pet = db.find(Pet.class, id);
    return pet;
  }

  /**
   * Add a new pet to the store.
   *
   * @param pet Pet object that needs to be added to the store.
   * @return Returns a saved pet.
   */
  @POST
  public Pet post(@Body final Pet pet) {
    db.save(pet);
    return pet;
  }

  /**
   * Update an existing pet.
   *
   * @param body Pet object that needs to be updated.
   * @return Returns a saved pet.
   */
  @PUT
  public Pet put(@Body final Pet pet) {
    db.save(pet);
    return pet;
  }

  /**
   * Deletes a pet by ID.
   *
   * @param id Pet ID.
   */
  public void delete(final int id) {
    db.delete(Pet.class, id);
  }
}
