package kt

import examples.Pet
import examples.PetQuery
import examples.PetRepo
import io.jooby.Context
import io.jooby.Kooby
import io.jooby.body
import io.jooby.form
import io.jooby.query
import io.jooby.require
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse

@Operation(requestBody = RequestBody(description = "Pet to create"))
fun createPet(ctx: Context): Pet {
  val repo = ctx.require(PetRepo::class)
  val pet = ctx.body<Pet>()
  return repo.save(pet)
}

fun updatePet(ctx: Context): Pet {
  val repo = ctx.require(PetRepo::class)
  val pet = ctx.body<Pet>()
  return repo.update(pet)
}

@ApiResponse(responseCode = "204")
fun deletePet(ctx: Context) {
  val repo = ctx.require(PetRepo::class)
  val id = ctx.path("id").longValue()
  repo.deleteById(id)
}

@Operation(
    summary = "Find a pet by ID",
    description = "Find a pet by ID or throws a 404",
    tags = ["find", "query"],
    parameters = [
      Parameter(
          description = "Pet ID",
          `in` = ParameterIn.PATH
      )
    ]
)
@ApiResponse
fun findPetById(ctx: Context): Pet {
  val repo = ctx.require(PetRepo::class)
  val id = ctx.path("id").longValue()
  return repo.findById(id)
}

fun formPet(ctx: Context): Pet {
  val repo: PetRepo = ctx.require(PetRepo::class)
  val pet = ctx.form<Pet>()
  return repo.save(pet)
}

class KtMinApp : Kooby({

  path("/api/pets") {
    get("/") {
      val repo = ctx.require(PetRepo::class)
      val query = ctx.query<PetQuery>()
      repo.pets(query)
    }

    get("/{id}", ::findPetById)

    post("/", ::createPet)

    put("/", ::updatePet)

    patch("/", ::updatePet)

    delete("/{id}", ::deletePet)

    post("/form", ::formPet)
  }


})
