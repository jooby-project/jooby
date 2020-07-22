package issues.i1855;

import examples.Person;
import io.jooby.MediaType;
import io.jooby.annotations.GET;
import io.jooby.annotations.Path;
import io.jooby.annotations.PathParam;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Path("/openapi")
public class Controller1855 {

  @GET("/{id}")
  @ApiResponse(responseCode = "200", content = {
      @Content(mediaType = MediaType.JSON, schema = @Schema(type = "object", oneOf = { Person.class }))
  })
  public Person get(@PathParam long id) {
    return null;
  }
}
