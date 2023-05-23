/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package kt

import examples.Pet
import examples.PetRepo
import io.jooby.Context
import io.jooby.kt.Kooby
import io.jooby.kt.body
import io.jooby.kt.require
import io.swagger.v3.oas.annotations.Operation

class KtRouteRef : Kooby({ post("/", ::createPetRef) })

@Operation(summary = "Create a Pet", description = "aaa")
fun createPetRef(ctx: Context) {
  val repo = ctx.require(PetRepo::class)
  val pet = ctx.body<Pet>()
}
