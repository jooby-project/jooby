/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i1601;

import static io.jooby.openapi.MvcExtensionGenerator.toMvcExtension;

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
    info =
        @Info(
            title = "Title",
            description = "description",
            termsOfService = "Terms",
            contact =
                @Contact(name = "Jooby", url = "https://jooby.io", email = "support@jooby.io"),
            license = @License(name = "Apache", url = "https://jooby.io/LICENSE"),
            version = "10"),
    tags = @Tag(name = "mytag"),
    servers =
        @Server(
            url = "serverurl",
            description = "...",
            variables =
                @ServerVariable(
                    name = "env",
                    defaultValue = "dev",
                    allowableValues = {"stage", "prod"},
                    description = "environment")),
    security = @SecurityRequirement(name = "oauth", scopes = "read:write"))
public class App1601b extends Jooby {
  {
    mvc(toMvcExtension(Controller1601.class));
  }
}
