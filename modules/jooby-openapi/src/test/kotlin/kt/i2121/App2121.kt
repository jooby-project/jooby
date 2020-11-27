package kt.i2121

import io.jooby.Kooby

class App2121 : Kooby({

  coroutine {
    mvc(Controller2121())
  }
})
