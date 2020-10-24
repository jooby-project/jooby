package issues.i2046;

import io.jooby.Jooby;

public class App2046 extends Jooby {
  {
    get("/2046", ctx -> "..");

    mount("/b", new App2046b());
  }
}
