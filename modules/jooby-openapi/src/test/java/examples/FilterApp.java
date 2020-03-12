package examples;

import io.jooby.Jooby;

public class FilterApp extends Jooby {

  {
    get("/", ctx -> "Welcome");

    get("/profile", ctx -> "Profile");

    path("/api", () -> {

      get("/profile", ctx -> "profile");

    });

    get("/api/profile/{id}", ctx -> "profile ID");
  }
}
