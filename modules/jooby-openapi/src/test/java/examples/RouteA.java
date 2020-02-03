package examples;

import io.jooby.Context;
import io.jooby.Jooby;

public class RouteA extends Jooby {

  {
    get("/a/1", Context::getRequestPath);
  }
}
