/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3461;

import static io.jooby.openapi.MvcExtensionGenerator.toMvcExtension;

import java.util.UUID;

import io.jooby.Jooby;
import io.jooby.annotation.GET;
import io.jooby.annotation.Path;
import io.jooby.annotation.QueryParam;
import io.swagger.v3.oas.annotations.Parameter;

public class App3461 extends Jooby {

  @Path("/")
  static class Controller {

    @GET("/3461")
    public String getBlah(
        @Parameter(required = true, description = "Param annotation") @QueryParam UUID orgId) {
      return "";
    }
  }

  {
    mvc(toMvcExtension(Controller.class));
  }
}
