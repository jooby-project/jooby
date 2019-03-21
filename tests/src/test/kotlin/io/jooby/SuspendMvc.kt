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
    return ctx.pathString()
  }

  @GET
  @Path("/{id}")
  suspend fun pathParam(@PathParam id: Int): Any {
    return id;
  }
}
