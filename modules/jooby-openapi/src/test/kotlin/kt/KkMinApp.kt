/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package kt

import examples.Pet
import examples.PetQuery
import examples.PetRepo
import io.jooby.Context
import io.jooby.kt.Kooby
import io.jooby.kt.body
import io.jooby.kt.form
import io.jooby.kt.query
import io.jooby.kt.require
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse

@Operation(requestBody = RequestBody(description = "Pet to create"))
fun createPet(ctx: Context): Pet {
  val repo = ctx.require(PetRepo::class)
  val pet = ctx.body(Pet::class)
  return repo.save(pet)
}

fun updatePet(ctx: Context): Pet {
  val repo = ctx.require(PetRepo::class)
  val pet = ctx.body(Pet::class)
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
  parameters = [Parameter(description = "Pet ID", `in` = ParameterIn.PATH)]
)
@ApiResponse
fun findPetById(ctx: Context): Pet {
  val repo = ctx.require(PetRepo::class)
  val id = ctx.path("id").longValue()
  return repo.findById(id)
}

fun formPet(ctx: Context): Pet {
  val repo: PetRepo = ctx.require(PetRepo::class)
  val pet = ctx.form(Pet::class)
  return repo.save(pet)
}

class KtMinApp :
  Kooby({
    path("/api/pets") {
      get("/") {
        val repo = ctx.require(PetRepo::class)
        val query = ctx.query(PetQuery::class)
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
