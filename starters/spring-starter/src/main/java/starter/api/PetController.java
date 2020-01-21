package starter.api;

import io.jooby.annotations.GET;
import io.jooby.annotations.POST;
import io.jooby.annotations.Path;
import io.jooby.annotations.PathParam;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import starter.domain.Pet;
import starter.domain.PetRepository;

import javax.inject.Inject;
import java.util.List;

@Controller
@Transactional
@Path("/pets")
public class PetController {

  private PetRepository repository;

  @Inject
  public PetController(PetRepository repository) {
    this.repository = repository;
  }

  @GET
  public List<Pet> findAll() {
    return repository.findAll();
  }

  @GET("/{name}")
  public Pet findByName(@PathParam String name) {
    return repository.findByName(name);
  }

  @POST
  public Pet create(Pet pet) {
    return repository.save(pet);
  }
}
