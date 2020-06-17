package issues.i1794;

import io.jooby.StatusCode;
import io.jooby.annotations.GET;
import io.jooby.annotations.POST;
import io.jooby.annotations.Path;
import io.jooby.annotations.PathParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

public class Controller1794 {
  @POST
  @Operation(
      summary = "This is the post for testSuite",
      description = "TestSuite change1234 Description",
      operationId = "User creation"
  )
  @ApiResponse(responseCode = "204", description = "Created")
  public StatusCode create(
      @RequestBody(required = true, description = "This is the requestBody") TestSuite testSuite) {
    return StatusCode.NO_CONTENT;
  }

  @GET
  @Path("/{code}")
  @Operation(description = "This is the description of the conducted application", summary = "Get testSuite")
  public TestSuite find(@Parameter(name = "code", required = true) @PathParam String code) {
    TestSuite testSuite = new TestSuite();
    return testSuite;
  }
}
