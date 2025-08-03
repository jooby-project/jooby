/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package kt.i3746

import io.jooby.ExecutionMode
import io.jooby.kt.runApp

fun main(args: Array<String>) {
  runApp(args, Server3746(), ExecutionMode.WORKER) { get("/3746") { ctx.requestPath } }
}
