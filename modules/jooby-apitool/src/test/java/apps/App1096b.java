package apps;

import org.jooby.Jooby;

public class App1096b extends Jooby {

  {
    path("1096", () -> {
      get("/search", req -> {
        return req.params(Param1096.class).toString();
      });
    });
  }
}
