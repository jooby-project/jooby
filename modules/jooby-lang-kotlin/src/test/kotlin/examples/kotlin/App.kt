package examples.kotlin

import org.jooby.*

/**
 * Use Kooby to make Jooby more Kotlin.
 */
class App: Kooby() {
  /**
   * Configure your application
   */
  init {
    get ("/") {
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