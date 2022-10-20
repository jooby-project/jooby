/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i1596;

import java.util.List;

import io.jooby.annotations.GET;
import io.jooby.annotations.POST;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;

@Tags(@Tag(name = "pets", description = "Group pets"))
public class ClassLevelController {
  @GET
  @Operation(summary = "Get Pets")
  @Tag(name = "query", description = "Search API")
  public List<String> getPets() {
    return null;
  }

  @POST
  @Operation(summary = "Create Pets")
  public String createPets() {
    return null;
  }
}
