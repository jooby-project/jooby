  package examples.kotlin

import org.jooby.*

class KApp: Kooby({

  use({env, conf, binder ->
    env.onStart {->
      println("Start from module")
    }
    println("XXX New module $env $conf $binder")
  })

  get {
    "Hi Kotlin"
  }

  onStart {
    println("Starting")
  }

  onStarted {
    println("Started")
  }

  onStop {
    println("Stopped")
  }

  path("/") {
    get {->
      val value = param("x").value
      "x $value"
    }
  }
})

/**
 * Start Jooby
 */
fun main(args: Array<String>) {
  Jooby.run(::App, args)
}
