/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i2594;

import static io.jooby.openapi.MvcExtensionGenerator.toMvcExtension;

import io.jooby.Jooby;

public class ControllersAppV12594 extends Jooby {

  public ControllersAppV12594() {
    mvc(toMvcExtension(ControllerV12594.class));
  }
}
