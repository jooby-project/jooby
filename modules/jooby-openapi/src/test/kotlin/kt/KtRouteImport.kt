/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package kt

import examples.RouteA
import io.jooby.Kooby
import io.jooby.require

class KtRouteImport :
  Kooby({
    mount(RouteA())
    path("/main") {
      mount(RouteA())
      mount("/submain", RouteA())
    }

    mount(RouteA())
    mount("/require", require(RouteA::class))
    mount("/subroute", RouteA())
  })
