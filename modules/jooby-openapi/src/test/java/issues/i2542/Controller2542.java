package issues.i2542;

import javax.annotation.Nullable;

import io.jooby.annotations.GET;
import io.jooby.annotations.Path;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

public class Controller2542 {
  @GET
  @Path("/2542")
  public @Nullable byte[] byteArray() {
    return new byte[0];
  }

  @GET("/2542/annotation")
  @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = byte.class, nullable = true))))
  public byte[] byteArrayWithAnnotation() {
    return new byte[0];
  }
}
