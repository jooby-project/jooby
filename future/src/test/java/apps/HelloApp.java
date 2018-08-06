package apps;

import io.jooby.App;
import io.jooby.Mode;
import io.jooby.jetty.Jetty;
import io.jooby.netty.Netty;
import io.jooby.utow.Utow;

public class HelloApp extends App {

  {
    get("/", ctx -> "Hello World!");

    get("/{foo}", ctx -> "Hello World!");

    dispatch(() -> {
      get("/worker", ctx -> "Hello Worker");
    });

    error((ctx, cause, statusCode) -> {
      ctx.statusCode(statusCode)
          .send(statusCode.reason());
    });
  }

  public static void main(String[] args) {
    HelloApp app = new HelloApp();
    app.mode(Mode.IO);
    app.use(new Utow());
    app.start();
  }
}
