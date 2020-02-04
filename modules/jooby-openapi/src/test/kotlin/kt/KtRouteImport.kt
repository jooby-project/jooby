package kt

import examples.RouteA
import io.jooby.Kooby
import io.jooby.require


class KtRouteImport : Kooby({
  use(RouteA())
  path("/main") {
    use(RouteA())
    use("/submain", RouteA())
  }

  use(RouteA())
  use("/require", require(RouteA::class))
  use("/subroute", RouteA())

})
