package kt

import io.jooby.Kooby
import io.swagger.v3.oas.annotations.tags.Tag


@Tag(name = "super")
class KtPathOperatorWithTags : Kooby({

  path("/pets") {
    get("/") {
      "..."
    }
        .tags("local")
        .summary("List pets")
        .description("Pets ...")
  }
      .tags("top")
      .summary("API summary")
      .description("API description")
})
