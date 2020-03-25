package examples;

import io.jooby.Context;
import io.jooby.Jooby;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;

@OpenAPIDefinition(
    info = @Info(
        extensions = @Extension(name = "api-json", properties = @ExtensionProperty(name = "properties", value = "{enabled: true}", parseValue = true))
    )
)
@Tags(@Tag(name = "tag", extensions = @Extension(name = "tag", properties = @ExtensionProperty(name = "properties", value = "{value: 45}", parseValue = true))))
public class ExtensionApp extends Jooby {
  {
    get("/op/{q}", this::extension);
  }

  @Operation(extensions = {
      @Extension(properties = {
          @ExtensionProperty(name = "x", value = "y"),
          @ExtensionProperty(name = "y", value = "z")
      })
  })
  @Parameters(@Parameter(name = "q", extensions = @Extension(properties = @ExtensionProperty(name = "q", value = "ext"))))
  @ApiResponse(extensions = @Extension(properties = @ExtensionProperty(name = "rsp", value = "ext")))
  private String extension(Context context) {
    return context.path("q").value();
  }
}
