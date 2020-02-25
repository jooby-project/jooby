package kt

import examples.Pet
import examples.PetQuery
import examples.PetRepo
import io.jooby.Context
import io.jooby.Kooby
import io.jooby.body
import io.jooby.require
import io.jooby.query
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.responses.ApiResponse

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
  }


})
