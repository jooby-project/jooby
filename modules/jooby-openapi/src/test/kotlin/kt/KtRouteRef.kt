package kt

import examples.Pet
import examples.PetRepo
import io.jooby.Context
import io.jooby.Kooby
import io.jooby.body
import io.jooby.require
import io.swagger.v3.oas.annotations.Operation


@Operation(summary = "Create a Pet")
fun createPetRef(ctx: Context) {
  val repo = ctx.require(PetRepo::class)
  val pet = ctx.body<Pet>()
}

class KtRouteRef : Kooby({

  post("/", ::createPetRef)

})
