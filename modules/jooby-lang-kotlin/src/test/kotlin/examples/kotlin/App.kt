package examples.kotlin

import org.jooby.Jooby
import org.jooby.Kooby

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