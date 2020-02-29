package examples;

import io.jooby.Context;
import io.jooby.Jooby;
import io.swagger.v3.oas.annotations.Operation;

public class NoAppClass {
  public static void main(String[] args) {
    Jooby.runApp(args, app -> {

      app.get("/path", ctx -> "Foo");

      app.get("/fn", NoAppClass::fnRef);

    });
  }

  @Operation(summary = "function reference")
  public static int fnRef(Context ctx) {
    return 0;
  }
}
