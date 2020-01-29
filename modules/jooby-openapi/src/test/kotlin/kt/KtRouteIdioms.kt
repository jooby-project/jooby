package kt

import io.jooby.Kooby

class KtRouteIdioms : Kooby({

  get("/implicitContext") {
    "OK"
  }

})
