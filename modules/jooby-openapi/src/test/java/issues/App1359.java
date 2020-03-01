package issues;

import io.jooby.Context;
import io.jooby.Jooby;
import io.jooby.MediaType;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

public class App1359 extends Jooby {

  {
    get("/script/1359", this::defaultResponse)
        .produces(MediaType.text);

    mvc(new Controller1359());
  }

  @ApiResponses({
      @ApiResponse(description = "This is the default response"),
      @ApiResponse(responseCode = "500"),
      @ApiResponse(responseCode = "400"),
      @ApiResponse(responseCode = "404")
  })
  public String defaultResponse(Context ctx) {
    return null;
  }
}
