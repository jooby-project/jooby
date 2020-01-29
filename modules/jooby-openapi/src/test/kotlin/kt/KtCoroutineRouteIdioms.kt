package kt

import io.jooby.Kooby

class KtCoroutineRouteIdioms : Kooby({

  coroutine {
    get("/version") {
      "version"
    }

    path("/api") {
      get("/version") {
        "version"
      }

      get("/people") {
        val q = ctx.query("search").value()
        q
      }
    }
  }
})
