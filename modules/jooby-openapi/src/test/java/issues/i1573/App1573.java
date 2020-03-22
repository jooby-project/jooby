package issues.i1573;

import io.jooby.Jooby;

public class App1573 extends Jooby {
  {
    get("/profile/{id}?", ctx -> {
      return ctx.path("id").value("self");
    });

    mvc(new Controller1573());
  }
}
