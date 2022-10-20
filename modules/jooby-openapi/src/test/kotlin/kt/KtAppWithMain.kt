/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package kt

import io.jooby.Kooby
import io.jooby.OpenAPIModule
import io.jooby.runApp

class App :
  Kooby({
    install(OpenAPIModule())

    get("/welcome") { "Welcome to Jooby!" }
  })

fun main(args: Array<String>) {
  runApp(args, App::class)
}
