package kt

import org.jooby.*

class CompApp : Kooby({

  get("/r1") {
    true
  }

  use(Comp1())

  use(Comp2())
})
