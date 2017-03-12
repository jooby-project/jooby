package examples.kotlin

import org.jooby.Jooby

class App: Jooby() {
  /**
   * Configure your application
   */
  init {

    get ("/") { ->
      "Hi Kotlin"
    }
  }
}

/**
 * Start Jooby
 */
fun main(args: Array<String>) {
  Jooby.run(::App, args)
}