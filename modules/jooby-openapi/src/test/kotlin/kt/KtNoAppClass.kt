/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package kt

import io.jooby.Context
import io.jooby.runApp
import io.swagger.v3.oas.annotations.Operation

@Operation(summary = "function reference")
fun fnRef(ctx: Context): Int {
  return 0
}

fun main(args: Array<String>) {
  runApp(args) {
    get("/path") { "Foo" }

    get("/fn", ::fnRef)
  }
}
