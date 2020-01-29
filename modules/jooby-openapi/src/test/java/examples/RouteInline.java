package examples;

import io.jooby.Jooby;

public class RouteInline {

  public static void main(String[] args) {
    Jooby.runApp(args, app -> {
      app.get("/inline", ctx -> "...");
    });
  }
}
