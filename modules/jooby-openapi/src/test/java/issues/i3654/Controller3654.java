/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3654;

import java.util.Map;

import io.jooby.annotation.GET;
import io.jooby.annotation.Path;
import io.jooby.annotation.PathParam;
import io.jooby.annotation.QueryParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

@Path("/3652")
public class Controller3654 {

  @GET("/{id}")
  @Operation(summary = "Find a user by ID", description = "Finds a user by ID or throws a 404")
  public Map<String, Object> getUser(
      @Parameter(description = "The user ID", required = true) @PathParam String id,
      @Parameter(
              description =
                  "Flag for fetching active/inactive users. (Defaults to true if not provided)")
          @QueryParam
          Boolean activeOnly) {
    return null;
  }
}
