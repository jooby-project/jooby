/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3412;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Jooby;
import io.jooby.OpenAPIModule;
import io.jooby.annotation.GET;
import io.jooby.annotation.Path;
import io.jooby.annotation.QueryParam;

public class App3412 extends Jooby {

  @Path("/")
  static class Controller {

    @GET("/welcome")
    public String sayHi(@QueryParam @NonNull String greeting, @QueryParam String language) {
      return "hi";
    }
  }

  {
    install(new OpenAPIModule());

    mvc(new Controller());
  }
}
