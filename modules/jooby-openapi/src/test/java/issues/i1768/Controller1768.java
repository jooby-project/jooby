/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i1768;

import io.jooby.annotation.GET;
import io.jooby.annotation.Path;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;

@Path("/c")
public class Controller1768 {

  @Hidden
  @GET("/hidden-with-annotation")
  public void hiddenWithAnnotation() {}

  @Operation(hidden = true)
  @GET("/hidden-with-operation")
  public void hiddenWithOperation() {}

  @GET("/not-hidden")
  public void notHidden() {}
}
