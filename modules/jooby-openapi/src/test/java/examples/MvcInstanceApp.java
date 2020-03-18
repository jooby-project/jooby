package examples;

import io.jooby.Jooby;

public class MvcInstanceApp extends Jooby {

  {
    mvc(new ControllerExample());
  }
}
