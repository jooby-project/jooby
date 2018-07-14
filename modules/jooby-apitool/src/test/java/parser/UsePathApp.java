package parser;

import org.jooby.Jooby;

import java.util.ArrayList;
import java.util.List;

public class UsePathApp extends Jooby {

  {

    /** /api/path */
    path("/api/path", () -> {
      /** GET /api/path */
      get(() -> {
        List<Foo> foos = new ArrayList<>();
        return foos;
      });
      /** GET /api/path/:id */
      get("/:id", req -> {
        int id = req.param("id").intValue();
        return new Foo(id);
      });
    });
  }
}
