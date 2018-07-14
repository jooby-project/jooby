package apps;

import org.jooby.Jooby;
import org.jooby.MediaType;
import org.jooby.apitool.Cat;

public class App537 extends Jooby {

  public App537() {

    /**
     * Produces Cat object
     *
     * Next line
     */
    path("/api/cat/", () -> {
        /**
         * @param name Cat's name
         *
         * @return Returns a cat {@link Cat}
         */
        get("/:name", req -> {
          Cat cat = new Cat();
          cat.setName(req.param("name").value());
          return cat;
        })
        .produces(MediaType.json);
    });
  }
}
