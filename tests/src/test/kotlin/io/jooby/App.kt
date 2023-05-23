/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby

import io.jooby.kt.getValue
import io.jooby.kt.runApp

data class SearchQuery(val q: String)

fun main(args: Array<String>) {
  runApp(args) {
    get("/") {
      val q: List<String> by ctx.query()
      q
    }
  }
}
