package apps;

import org.jooby.Jooby;

public class NoArgHandler extends Jooby {

  {
    get("/", () -> {
      return 0;
    });
  }

}
