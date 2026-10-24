/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.mvc

import io.jooby.Context
import io.jooby.ServerSentEmitter
import io.jooby.annotation.Path
import io.jooby.annotation.PathParam
import io.jooby.annotation.SSE

@Path("/kotlin-sse")
class KotlinSseController {

  @SSE("/events", "/alias")
  fun events(emitter: ServerSentEmitter) {
    emitter.send("Welcome")
    emitter.close()
  }

  @SSE(path = ["/{name}"])
  fun named(ctx: Context, @PathParam name: String, emitter: ServerSentEmitter): Unit {
    emitter.send("$name:${ctx.requestPath}")
    emitter.close()
  }

}
