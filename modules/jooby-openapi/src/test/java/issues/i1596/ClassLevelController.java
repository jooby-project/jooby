package issues.i1596;

import io.jooby.annotations.GET;
import io.jooby.annotations.POST;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;

import java.util.List;

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
