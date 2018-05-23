package apps;

import org.jooby.Jooby;

public class App1096c extends Jooby {

  {
    path("1096", () -> {
      post("/form", req -> {
        return req.form(Form1096.class).toString();
      });
    });
  }
}
