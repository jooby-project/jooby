/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3652;

import io.jooby.annotation.GET;
import io.jooby.annotation.Path;
import io.jooby.annotation.PathParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@Path("/3652")
@SecurityScheme(
    name = "myBearerToken",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    in = SecuritySchemeIn.HEADER)
public class Controller3652 {

  @GET("/{id}")
  @Operation(summary = "Find a user by ID", description = "Finds a user by ID or throws a 404")
  @SecurityRequirement(name = "myBearerToken", scopes = "user:read")
  public String sayHi(@PathParam String id) {
    return "hi";
  }
}
