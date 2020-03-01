package issues;

import io.jooby.Context;
import io.jooby.annotations.GET;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

public class Controller1359 {

  @GET("/controller/1359")
  @ApiResponses({
      @ApiResponse(description = "This is the default response", content = @Content(mediaType = "text/plain")),
      @ApiResponse(responseCode = "500"),
      @ApiResponse(responseCode = "400"),
      @ApiResponse(responseCode = "404")
  })
  public String defaultResponse(Context ctx) {
    return null;
  }

  @GET("/controller/1359/missing")
  @ApiResponses({
      @ApiResponse(responseCode = "500"),
      @ApiResponse(responseCode = "400"),
      @ApiResponse(responseCode = "404")
  })
  public String defaultResponseMissing(Context ctx) {
    return null;
  }

  @GET("/controller/1359/customcode")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "This is the default response", content = @Content(mediaType = "text/plain")),
      @ApiResponse(responseCode = "500"),
      @ApiResponse(responseCode = "400"),
      @ApiResponse(responseCode = "404")
  })
  public String customStatusCode(Context ctx) {
    return null;
  }

  @GET("/controller/1359/multiplesuccess")
  @ApiResponses({
      @ApiResponse(content = @Content(mediaType = "text/plain")),
      @ApiResponse(responseCode = "201", content = @Content(mediaType = "text/plain")),
      @ApiResponse(responseCode = "500"),
      @ApiResponse(responseCode = "400"),
      @ApiResponse(responseCode = "404")
  })
  public String multiplesuccess(Context ctx) {
    return null;
  }
}
