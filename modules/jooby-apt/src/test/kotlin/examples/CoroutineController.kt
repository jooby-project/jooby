package examples

import io.jooby.Context

class CoroutineController {
  suspend fun doSomething(ctx: Context) : String {
    return "ctx"
  }
}
