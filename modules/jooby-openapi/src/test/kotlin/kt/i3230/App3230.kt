/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package kt.i3230

import io.jooby.kt.Kooby
import io.jooby.kt.runApp

// an extension method that creates some routes
fun Kooby.helloRoute() {
  get("/hello") { "world" }

  post("/create") { "world" }
}

fun main(args: Array<String>) {
  runApp(args) {
    // calling the extension method
    helloRoute()
  }
}
