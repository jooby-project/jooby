/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3059;

import io.jooby.annotation.POST;
import io.jooby.annotation.Path;

@Path("/mvc/b")
public class ControllerB3059 {
  @POST
  public void pathB() {}
}
