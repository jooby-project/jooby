package issues;

import io.jooby.Jooby;

public class App1585 extends Jooby {
  {
    get("/user/{id:[0-9]+}", ctx -> ctx.path("id").intValue());

    get("/company/{id}", ctx -> ctx.path("id").intValue());

    get("/file/*", ctx -> ctx.path("*").value());

    get("/resources/*path", ctx -> ctx.path("path").value());
  }
}
