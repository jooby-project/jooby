package kt

import io.jooby.Kooby

class KtRouteIdioms : Kooby({

  get("/implicitContext") {
    "implicit"
  }

  get("/explicitContext") { ctx ->
    "explicit"
  }

  path("/api") {
    path("/people") {
      get("/") {
        ctx.requestPath
      }
    }

    get("/version") { ctx -> "2.6.0" }
  }
})
