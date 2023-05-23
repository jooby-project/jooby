/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.kt

import io.jooby.annotation.GET
import io.jooby.annotation.Path
import io.jooby.annotation.PathParam
import io.jooby.annotation.QueryParam

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
  fun queryBean(
    @QueryParam point: QueryPoint,
    @QueryParam x: String,
    @QueryParam y: String
  ): String {
    return point.toString() + ";" + x + ";" + y
  }
}
