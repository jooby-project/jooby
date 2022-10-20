/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
import io.jooby.ExecutionMode
import io.jooby.runApp

data class SearchQuery(val q: String)

fun main(args: Array<String>) {
  runApp(args, ExecutionMode.EVENT_LOOP) {
    serverOptions { ioThreads = 5 }
    get("/") { ":+1" }
  }
}
