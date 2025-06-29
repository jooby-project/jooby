/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3575;

import static io.jooby.openapi.MvcExtensionGenerator.toMvcExtension;

import io.jooby.Context;
import io.jooby.Jooby;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;

public class App3575 extends Jooby {
  {
    get("/", this::home);
    get("/hide-op", this::hideOp);

    mvc(toMvcExtension(Controller3575.class));
  }

  @Operation(hidden = true)
  private Object hideOp(Context context) {
    return null;
  }

  @Hidden
  private Object home(Context context) {
    return null;
  }
}
