package examples

import io.jooby.Kooby

class CoroutineApp: Kooby({
  coroutine {
    get {
      CoroutineController().doSomething(ctx)
    }
  }
})
