/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package kt

import io.jooby.Kooby
import kotlinx.coroutines.delay

class KtCoroutineRouteIdioms :
  Kooby({
    coroutine {
      get("/version") {
        delay(100)
        "version"
      }

      path("/api") {
        patch("/version") { "version" }

        get("/people") { "people" }
      }
    }
  })
