/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package kt.i2121

import io.jooby.annotation.GET
import io.jooby.annotation.Path
import io.jooby.annotation.QueryParam
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import kotlinx.coroutines.delay

@Path("/")
class Controller2121 {

  @Operation(
    summary = "Values for single ID",
    description = "Delivers full data for an ID for a given year",
    parameters =
      [
        Parameter(
          name = "year",
          example = "2018",
          description = "The year where the data will be retrieved",
          required = true,
        ),
        Parameter(
          name = "id",
          example = "XD12345",
          description = "An ID value which belongs to a dataset",
          required = true,
        ),
      ],
  )
  @GET
  suspend fun listDataForID(@QueryParam year: Int, @QueryParam id: String): String {
    delay(1000L)
    return "Welcome to Jooby! Year=${year} and ID=${id}"
  }
}
