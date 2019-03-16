package apps

import io.jooby.ExecutionMode
import io.jooby.Kooby
import io.jooby.run
import kotlinx.coroutines.delay

/** Class version: */
class App : Kooby({


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
})

/** run class: */
fun runClass(args: Array<String>) {
  run(::App, args)
}

/** run class with mode: */
fun runWithMode(args: Array<String>) {
  run(::App, ExecutionMode.DEFAULT, args)
}

/** run inline: */
fun runInline(args: Array<String>) {
  run(args) {
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
