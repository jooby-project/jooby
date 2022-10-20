/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby

import io.jooby.annotations.GET
import io.jooby.annotations.Path
import io.jooby.annotations.PathParam
import kotlinx.coroutines.delay

class SuspendMvc {
  @GET
  suspend fun getIt(): String {
    return "Got it!"
  }

  @GET
  @Path("/delay")
  suspend fun delayed(ctx: Context): String {
    delay(100)
    return ctx.getRequestPath()
  }

  @GET
  @Path("/{id}")
  suspend fun pathParam(@PathParam id: Int): Any {
    return id
  }
}
