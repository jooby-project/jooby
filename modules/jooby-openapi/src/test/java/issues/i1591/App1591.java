package issues.i1591;

import io.jooby.Context;
import io.jooby.Jooby;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

public class App1591 extends Jooby {
  {
    get("/1591", this::parameterExample);
  }

  @Operation(
      //this value does get respected
      summary = "Example.",
      security = @SecurityRequirement(name = "basicAuth"),
      //this value doesn't get respected
      parameters = {
          @Parameter(name = "arg1", description = "Arg 1", example = "ex1", in = ParameterIn.PATH),
          @Parameter(name = "arg2",
              examples = {@ExampleObject("ex2"), @ExampleObject("ex3")}, in = ParameterIn.PATH
          )
      }
  )
  private String parameterExample(Context context) {
    String arg1 = context.path("arg1").value();
    String arg2 = context.path("arg2").value();
    return null;
  }
}
