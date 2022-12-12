/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package i2465

import io.jooby.Context
import io.jooby.annotations.GET
import io.jooby.annotations.Path
import kotlinx.coroutines.delay

class C2465 {
  @GET
  @Path("/fun/2465")
  suspend fun delayed(ctx: Context): String {
    delay(100)
    return ctx.getRequestPath()
  }
}
