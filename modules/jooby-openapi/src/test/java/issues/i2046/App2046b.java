package issues.i2046;

import io.jooby.Jooby;

public class App2046b extends Jooby {

  {
    get("/2046b", ctx -> "..");
  }
}
