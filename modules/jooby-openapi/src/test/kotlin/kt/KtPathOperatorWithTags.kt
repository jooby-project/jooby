/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package kt

import io.jooby.kt.Kooby
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "super")
class KtPathOperatorWithTags :
  Kooby({
    path("/pets") { get("/") { "..." }.tags("local").summary("List pets").description("Pets ...") }
      .tags("top")
      .summary("API summary")
      .description("API description")
  })
