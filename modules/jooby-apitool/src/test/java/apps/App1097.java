package apps;

import org.jooby.Jooby;

public class App1097 extends Jooby {

  {
    path("/v5/api", () -> {
      path("/song", () -> {
        get(() -> "...");
      });
      path("/album", () -> {
        get(() -> "...");
      });
      path("/artist", () -> {
        get(() -> "...");
      });
    });
  }
}
