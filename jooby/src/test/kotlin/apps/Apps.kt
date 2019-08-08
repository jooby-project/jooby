package apps

import io.jooby.ExecutionMode
import io.jooby.Kooby
import io.jooby.runApp
import kotlinx.coroutines.delay

/** Class version: */
class App : Kooby({

  coroutine {
    get { "Hi Kotlin!" }

    get("/suspend") {
      delay(100)
      "Hi Coroutine"
    }

    get("/ctx-access") {
      ctx.pathString()
    }
  }

  get("/ctx-arg") { ctx ->
    ctx.pathString()
  }
})

/** run class: */
fun runClass(args: Array<String>) {
  runApp(args, App::class)
}

/** run class with mode: */
fun runWithMode(args: Array<String>) {
  runApp(args, ExecutionMode.DEFAULT, App::class)
}

/** run inline: */
fun runInline(args: Array<String>) {
  runApp(args) {
    coroutine {
      get { "Hi Kotlin!" }

      get("/suspend") {
        delay(100)
        "Hi Coroutine"
      }

      get("/ctx-access") {
        ctx.pathString()
      }

      get("/ctx-arg") { ctx ->
        ctx.pathString()
      }
    }
  }
}
