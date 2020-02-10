package examples;

import io.jooby.Jooby;

public class MvcApp extends Jooby {

  {
    mvc(ControllerExample.class);
  }
}
