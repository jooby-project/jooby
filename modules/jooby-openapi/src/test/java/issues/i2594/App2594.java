/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i2594;

import static io.jooby.openapi.MvcExtensionGenerator.toMvcExtension;

import io.jooby.Jooby;
import io.jooby.OpenAPIModule;

public class App2594 extends Jooby {
  {
    install(new OpenAPIModule());

    mvc(toMvcExtension(HealthController2594.class));

    mount("/api/v1", new ControllersAppV12594());
    mount("/api/v2", new ControllersAppV22594());
  }
}
