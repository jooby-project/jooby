/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3059;

import io.jooby.Jooby;
import io.jooby.openapi.OpenAPIGenerator;

public class App3059B extends Jooby {
  {
    IndirectRunner.create()
        .bindResource(new ControllerA3059())
        .bindResource(new ControllerC3059())
        .run();

    OpenAPIGenerator.registerMvc(ControllerA3059.class);
    OpenAPIGenerator.registerMvc(ControllerC3059.class);
  }
}
