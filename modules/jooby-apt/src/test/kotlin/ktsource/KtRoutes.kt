package ktsource

import io.jooby.annotations.GET
import io.jooby.annotations.Path

@Path("/kt")
class KtRoutes {
  @GET
  fun doIt() : String {
    return "..."
  }
}
