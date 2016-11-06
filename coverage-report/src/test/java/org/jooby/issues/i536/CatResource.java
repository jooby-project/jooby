package org.jooby.issues.i536;

import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Session;
import org.jooby.issues.i378.Cat;
import org.jooby.mvc.Flash;
import org.jooby.mvc.GET;
import org.jooby.mvc.Local;
import org.jooby.mvc.Path;
import org.jooby.mvc.Produces;

/**
 * Produces Cat object
 *
 * Next line
 */
public class CatResource {
  /**
   * Another description
   *
   * Another line
   *
   * @param name Cat's name
   * @return Returns a cat {@link Cat}
   */
  @Path("/api/cat/:name")
  @Produces("application/json")
  @GET
  public Cat get(final Request req, final Response response, final Session session, final Route route, final String name,
      @Flash final int flashParam, @Local final double localParam) {
    Cat cat = new Cat();
    cat.setName(name);

    return cat;
  }
}
