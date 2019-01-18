package apps;

import org.jooby.Jooby;

public class App1276b extends Jooby {

  {
    use(Controller1276.class);

    get("/foo", () -> { return "I'm listed"; });
  }
}
