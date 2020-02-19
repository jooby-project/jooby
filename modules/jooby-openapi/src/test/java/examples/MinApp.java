package examples;

import io.jooby.Context;
import io.jooby.Jooby;
import io.jooby.StatusCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

public class MinApp extends Jooby {
  {
    path("/api/pets", () -> {

      get("/", ctx -> {
        PetRepo repo = require(PetRepo.class);
        return repo.pets();
      });

      get("/{id}", this::findPetById);

      post("/", this::createPet);

      put("/", this::updatePet);

      patch("/", this::updatePet);

      delete("/{id}", this::deletePet);
    });
  }

  public Pet createPet(Context ctx) {
    PetRepo repo = require(PetRepo.class);

    Pet pet = ctx.body(Pet.class);
    return repo.save(pet);
  }

  public Pet updatePet(Context ctx) {
    PetRepo repo = require(PetRepo.class);

    Pet pet = ctx.body(Pet.class);
    return repo.update(pet);
  }

  @ApiResponse(responseCode = "204")
  public Context deletePet(Context ctx) {
    PetRepo repo = require(PetRepo.class);
    long id = ctx.path("id").longValue();
    repo.deleteById(id);
    return ctx.send(StatusCode.NO_CONTENT);
  }

  @Operation(summary = "Find a pet by ID", description = "Find a pet by ID or throws a 404")
  @ApiResponse
  public Pet findPetById(Context ctx) {
    PetRepo repo = require(PetRepo.class);
    long id = ctx.path("id").longValue();
    return repo.findById(id);
  }
}
