package kt

import io.jooby.Kooby
import kotlinx.coroutines.delay

class KtCoroutineRouteIdioms : Kooby({

  coroutine {
    get("/version") {
      delay(100)
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
