package kt

import io.jooby.Kooby

class KtCoroutineRouteIdioms : Kooby({

  coroutine {
    get("/version") {
      "version"
    }

    path("/api") {
      patch("/version") {
        "version"
      }

      get("/people") {
        "people"
      }
    }
  }
})
