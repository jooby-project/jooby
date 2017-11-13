package apps;

import org.jooby.Jooby;
import org.jooby.Results;

public class App946 extends Jooby {
  {
    /**
     * Top.
     */
    path("/some/path", () -> {

      path("/:id", () -> {
        /**
         * GET.
         * @param id Param ID.
         */
        get(req -> {
          return req.param("id").intValue();
        });

        /**
         * GET foo.
         * @param id Param ID.
         */
        get("/foo", req -> {
          return req.param("id").intValue();
        });
      });
    });
  }
}
