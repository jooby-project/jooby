package issues.i1601;

import io.jooby.Jooby;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.servers.ServerVariable;
import io.swagger.v3.oas.annotations.tags.Tag;

@OpenAPIDefinition(
    info = @Info(
        title = "Title",
        description = "description",
        termsOfService = "Terms",
        contact = @Contact(
            name = "Jooby",
            url = "https://jooby.io",
            email = "support@jooby.io"
        ),
        license = @License(
            name = "Apache",
            url = "https://jooby.io/LICENSE"
        ),
        version = "10"
    ),
    tags = @Tag(name = "mytag"),
    servers = @Server(
        url = "serverurl",
        description = "...",
        variables = @ServerVariable(name = "env", defaultValue = "dev", allowableValues = {
            "stage", "prod"}, description = "environment")
    ),
    security = @SecurityRequirement(name = "oauth", scopes = "read:write")
)
public class App1601 extends Jooby {
  {
    get("/1601", ctx -> "..");
  }
}
