package apps;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.ResponseHeader;
import org.jooby.Result;
import org.jooby.Results;
import org.jooby.mvc.GET;
import org.jooby.mvc.Path;

@Path("/api/1100")
public class Controller1100 {

  @GET
  @ApiOperation(value = "Summary", response = Pet.class, tags = {"foo",
      "bar"}, nickname = "listPets", notes = "List all pets", consumes = "foo/bar",
      produces = "foo/bar, bar/baz", httpMethod = "post", code = 202,
  responseHeaders = {@ResponseHeader(name ="foo", description = "a foo header", response = String.class)})
  public Result list() {
    return Results.ok();
  }

  @Path("/apiresponse")
  @GET
  @ApiOperation(value = "ApiResponse")
  @ApiResponse(code = 204, message = "Response message", response = Category.class, responseHeaders = {@ResponseHeader(name ="foo", description = "a foo header", response = String.class)})
  public Object listApiResponse() {
    return Results.ok();
  }

  @Path("/nresponse")
  @GET
  @ApiResponses({@ApiResponse(code = 200, message = "cat", response = Category.class),
      @ApiResponse(code = 200, message = "tag", response = Tag.class, responseHeaders = {@ResponseHeader(name ="foo", description = "a foo header", response = String.class)})})
  public Object listNApiResponse() {
    return Results.ok();
  }
}
