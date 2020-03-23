package issues.i1591;

import io.jooby.Context;
import io.jooby.Jooby;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;

public class App1591b extends Jooby {
  {
    get("/securityRequirements", this::securityRequirements);
    get("/securityRequirement", this::securityRequirement);
    get("/operationSecurityRequirement", this::operationSecurityRequirement);
  }

  @SecurityRequirements(
      @SecurityRequirement(name = "myOauth1", scopes = "write: read")
  )
  private String securityRequirements(Context context) {
    return null;
  }

  @SecurityRequirement(name = "myOauth2", scopes = "write: read")
  private String securityRequirement(Context context) {
    return null;
  }

  @Operation(
      security = @SecurityRequirement(name = "myOauth3", scopes = "write: read")
  )
  private String operationSecurityRequirement(Context context) {
    return null;
  }
}
