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
    use(RouteA())
    path("/main") {
      use(RouteA())
      use("/submain", RouteA())
    }

    use(RouteA())
    use("/require", require(RouteA::class))
    use("/subroute", RouteA())
  })
