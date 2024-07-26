/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package kt.i3483

import io.jooby.kt.Kooby

class App3483 : Kooby({ kotlinExtensionMethod() })

fun Kooby.kotlinExtensionMethod() {
  get("/some/*") { "OK" }

  get("/named-unused/*pathparam") { "OK" }
  get("/here") { "here" }
  mount("/api", object : Kooby({ get("/foo") { "OK" } }) {})

  mount(object : Kooby({ get("/foo") { "OK" } }) {})
}
