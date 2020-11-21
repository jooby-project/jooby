package kt.i1905

import io.jooby.Context
import io.jooby.Kooby

class SubApp1905 : Kooby({
  get("/sub") { ctx: Context? -> "OK" }
})
