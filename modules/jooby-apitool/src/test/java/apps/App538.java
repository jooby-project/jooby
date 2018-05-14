package apps;

import org.jooby.Jooby;
import org.jooby.apitool.Cat;
import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.mvc.Produces;

public class App538 extends Jooby {

  {
    use(CatResource.class);
  }

  /**
   * Produces Cat object
   *
   * Next line
   */
  @Path("/api/inner/cat")
  public static class CatResource {

    /**
     * @param name Cat's name
     * @return Returns a cat {@link Cat}
     */
    @Produces("application/json")
    @Path("/:name")
    @GET
    public Cat get(final String name) {
      Cat cat = new Cat();
      cat.setName(name);

      return cat;
    }
  }
}
