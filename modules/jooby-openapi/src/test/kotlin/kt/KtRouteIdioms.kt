/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package kt

import io.jooby.kt.Kooby

class KtRouteIdioms :
  Kooby({
    get("/implicitContext") { "implicit" }

    get("/explicitContext") { ctx -> ctx.requestPath }

    path("/api") {
      path("/people") { get("/") { ctx.requestPath } }

      get("/version") { ctx -> ctx.requestPath }
    }
  })
