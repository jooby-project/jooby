package examples;

import io.jooby.Context;
import io.jooby.Jooby;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.OAuthFlow;
import io.swagger.v3.oas.annotations.security.OAuthFlows;
import io.swagger.v3.oas.annotations.security.OAuthScope;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@SecurityScheme(
    name = "myOauth2Security",
    type = SecuritySchemeType.OAUTH2,
    in = SecuritySchemeIn.HEADER,
    flows = @OAuthFlows(
        implicit = @OAuthFlow(authorizationUrl = "http://url.com/auth",
            scopes = @OAuthScope(name = "write:pets", description = "modify pets in your account"))))
public class SecuritySchemeApp extends Jooby {
  {
    get("/ss", this::extension);
  }

  @SecurityRequirement(name = "myOauth2Security", scopes = "write: read")
  private String extension(Context context) {
    return context.path("q").value();
  }
}
