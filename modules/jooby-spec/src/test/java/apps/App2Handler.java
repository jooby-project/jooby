package apps;

import org.jooby.Jooby;

public class App2Handler extends Jooby {

  {

    get("/p1", "/p2", () -> null);

    get("/p3", "/p4", "/p5", () -> null);
  }
}
