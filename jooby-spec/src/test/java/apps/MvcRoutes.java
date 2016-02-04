package apps;

import java.util.List;
import java.util.Optional;

import org.jooby.mvc.Body;
import org.jooby.mvc.GET;
import org.jooby.mvc.POST;
import org.jooby.mvc.Path;

import apps.model.Pet;

@Path("/api/pets")
public class MvcRoutes {

  @Path("/:id")
  @GET
  public Pet get(final int id) {
    return null;
  }

  @GET
  public List<Pet> list(final Optional<Integer> start, final Optional<Integer> max) {
    return null;
  }

  @POST
  public Pet create(@Body final Pet pet) {
    return null;
  }
}
