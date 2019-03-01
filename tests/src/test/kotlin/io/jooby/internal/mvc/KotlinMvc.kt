package io.jooby.internal.mvc

import io.jooby.annotations.GET
import io.jooby.annotations.Path
import io.jooby.annotations.PathParam
import io.jooby.annotations.QueryParam

data class QueryPoint(val x: String?, val y: Int?)

@Path("/kotlin")
class KotlinMvc {

  @GET
  fun getIt(): String {
    return "Got it!"
  }

  @GET
  @Path("/{x}")
  fun pathParam(@PathParam x: Int): Int {
    return x
  }

  @GET
  @Path("/point")
  fun queryBean(@QueryParam point: QueryPoint, @QueryParam x: String?): String {
    return "$point : $x"
  }
}
