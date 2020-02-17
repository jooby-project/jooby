package examples;

import io.jooby.Jooby;

public class OpenApiApp extends Jooby {
  {
    mvc(new OpenApiController());
  }
}
