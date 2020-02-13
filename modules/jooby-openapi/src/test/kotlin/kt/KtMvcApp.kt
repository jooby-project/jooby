package kt

import io.jooby.Kooby

class KtMvcApp : Kooby({

  val provider = { KtController() }

  mvc(KtController::class, provider)

})
