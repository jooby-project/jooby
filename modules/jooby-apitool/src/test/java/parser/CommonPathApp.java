package parser;

import org.jooby.Jooby;

public class CommonPathApp extends Jooby {
  {
    /**
     * Doc.
     */
    path("/app", () -> {
      use("/foo", new FooApp());
      use("/bar", new FooApp());
    });

    /**
     * API Pets:
     */
    path("/api/pets", () -> {
      /**
       * List pets.
       */
      get(() -> "OK");

      /**
       * Find pet.
       */
      get("/:id", () -> "OK");
    });
  }
}
