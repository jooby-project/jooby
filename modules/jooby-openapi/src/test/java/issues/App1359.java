/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues;

import static io.jooby.openapi.MvcExtensionGenerator.toMvcExtension;

import io.jooby.Context;
import io.jooby.Jooby;
import io.jooby.MediaType;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

public class App1359 extends Jooby {

  {
    get("/script/1359", this::defaultResponse).produces(MediaType.text);
    mvc(toMvcExtension(Controller1359.class));
  }

  @ApiResponses({
    @ApiResponse(description = "This is the default response"),
    @ApiResponse(responseCode = "500"),
    @ApiResponse(responseCode = "400"),
    @ApiResponse(responseCode = "404")
  })
  public String defaultResponse(Context ctx) {
    return null;
  }
}
