package examples;

import io.jooby.Jooby;

public class MvcAppWithRoutes extends Jooby {

  {
    routes(() -> mvc(ControllerExample.class));
  }
}
