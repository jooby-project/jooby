package org.jooby.issues;

import org.jooby.Request;
import org.jooby.Response;
import org.jooby.issues.i378.Cat;
import org.jooby.mvc.GET;
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
  public Cat get(Request req, Response response, String name) {
    Cat cat = new Cat();
    cat.setName(name);

    return cat;
  }
}
