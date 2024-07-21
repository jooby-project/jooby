/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package i3477

import io.jooby.Context
import io.jooby.annotation.GET
import io.jooby.annotation.Transactional

class C3477 {
  @GET("/3477") @Transactional(false) fun generateToken(ctx: Context) = ctx.route.attributes
}
