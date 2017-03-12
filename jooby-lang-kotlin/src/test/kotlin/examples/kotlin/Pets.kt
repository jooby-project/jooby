package examples.kotlin

import org.jooby.mvc.*
import javax.inject.Inject

@Path("/api/pets")
class Pets @Inject constructor(val db: MyDatabase) {

  @GET
  fun list(): List<Pet> {
    return db.queryPets()
  }
}