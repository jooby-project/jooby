/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3729;

import java.util.Map;

import io.jooby.annotation.GET;
import io.jooby.annotation.Path;
import io.jooby.annotation.PathParam;
import io.jooby.annotation.QueryParam;

/**
 * Playing with API doc.
 *
 * <p>Sed eget orci imperdiet massa ultrices congue. Etiam ornare velit eu justo efficitur.
 */
@Path("/3729")
public class Controller3729 {

  /**
   * Find a user by ID. Finds a user by ID or throws a 404
   *
   * @param id The user ID.
   * @param activeOnly Flag for fetching active/inactive users. (Defaults to true if not provided).
   * @return Found user.
   */
  @GET("/{id}")
  public Map<String, Object> getUser(@PathParam String id, @QueryParam Boolean activeOnly) {
    return null;
  }
}
