package apps;

import java.util.List;
import java.util.Optional;

import org.jooby.mvc.GET;
import org.jooby.mvc.Path;

import apps.model.Pet;

/**
 * Api summary.
 *
 * @author edgar
 */
@Path("/api/pets")
public class PetMvc {

  /**
   * Get a pet by ID.
   *
   * @param id A pet ID.
   * @return A {@link Pet} with a <code>200</code> code or <code>404</code>.
   */
  @Path("/:id")
  @GET
  public Pet get(final int id) {
    return null;
  }

  /**
   * List pets.
   *
   * @param start Start offset.
   *        Optional
   * @param max Max number of results. Optional
   * @return List of pets in
   *         two lines.
   * @throws Exception If something goes wrong
   */
  @GET
  public List<Pet> list(final Optional<Integer> start, final Optional<Integer> max)
      throws Exception {
    return null;
  }
}
