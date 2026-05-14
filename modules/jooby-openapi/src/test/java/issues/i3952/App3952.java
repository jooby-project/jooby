/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3952;

import io.jooby.Context;
import io.jooby.Jooby;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;

public class App3952 extends Jooby {
  {
    path("/api", this::internalApiRoutes);
  }

  private void internalApiRoutes() {
    post("/getThing", App3952::getThing);
  }

  @Operation(
      summary = "Get a thing",
      parameters = {
        @Parameter(
            name = "x-api-key",
            description = "API Key",
            in = ParameterIn.HEADER,
            schema = @Schema(type = "string"),
            required = true),
        @Parameter(
            name = "x-bool",
            description = "Boolean key",
            in = ParameterIn.HEADER,
            schema = @Schema(type = "boolean")),
        @Parameter(
            name = "x-number",
            description = "Number key",
            in = ParameterIn.HEADER,
            schema = @Schema(type = "number")),
        @Parameter(
            name = "x-integer",
            description = "Int key",
            in = ParameterIn.HEADER,
            schema = @Schema(type = "integer"),
            required = true)
      })
  private static String getThing(Context context) {
    return "works!";
  }
}
