package examples;

import io.jooby.Context;
import io.jooby.Jooby;
import io.jooby.StatusCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

public class MinApp extends Jooby {
  {
    setContextPath("/myapp");

    path("/api/pets", () -> {

      get("/", ctx -> {
        PetRepo repo = require(PetRepo.class);
        PetQuery query = ctx.query(PetQuery.class);

        return repo.pets(query);
      });

      get("/{id}", this::findPetById);

      post("/", this::createPet);

      put("/", this::updatePet);

      patch("/", this::updatePet);

      delete("/{id}", this::deletePet);

      post("/form", this::formPet);
    });
  }

  public Pet formPet(Context context) {
    PetRepo repo = require(PetRepo.class);
    Pet pet = context.form(Pet.class);
    return repo.save(pet);
  }

  @Operation(requestBody = @RequestBody(description = "Pet to create"))
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

  @Operation(
      summary = "Find a pet by ID",
      description = "Find a pet by ID or throws a 404",
      tags = {"find", "query"},
      parameters = @Parameter(description = "Pet ID", in = ParameterIn.PATH)
  )
  public Pet findPetById(Context ctx) {
    PetRepo repo = require(PetRepo.class);
    long id = ctx.path("id").longValue();
    return repo.findById(id);
  }
}
