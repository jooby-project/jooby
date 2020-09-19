package kt

import io.jooby.Kooby

class KtMvcAppWithRoutes : Kooby({

  val provider = { KtController() }

  routes {
    mvc(KtController::class, provider)
  }
})
