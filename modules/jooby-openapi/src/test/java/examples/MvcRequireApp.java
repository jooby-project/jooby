/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import static io.jooby.openapi.MvcExtensionGenerator.toMvcExtension;

import io.jooby.Jooby;
import io.jooby.OpenAPIModule;
import io.jooby.annotation.GET;
import io.jooby.annotation.Path;

public class MvcRequireApp extends Jooby {

  @Path("/")
  static class Controller {

    @GET("/welcome")
    public String sayHi() {
      return "hi";
    }
  }

  {
    install(new OpenAPIModule());

    mvc(toMvcExtension(require(Controller.class)));
  }
}
